package it.vasilepersonalsite;

import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.service.impl.EmailServiceImpl;
import it.vasilepersonalsite.service.impl.GoogleMeetServiceImpl;
import it.vasilepersonalsite.util.HttpTimeouts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Garanzie sui timeout di Mailjet e Google: un timeout NON deve generare una seconda
 * richiesta (nessun retry nascosto) e il riuso del requestId Meet deve restare stabile.
 * Nessuna chiamata reale: server locali e MockRestServiceServer.
 */
class RetryEDuplicatiTest {

    private static final String MAILJET = "https://api.mailjet.com/v3.1/send";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events?conferenceDataVersion=1&sendUpdates=none";

    private PrenotazioneLezione lezione() {
        PrenotazioneLezione l = new PrenotazioneLezione();
        l.setId("lezione-xyz");
        l.setNomeStudente("Studente Test");
        l.setMateria("Analisi 1");
        l.setLivello("Universita");
        l.setDataLezione(LocalDate.of(2026, 11, 23));
        l.setOrarioInizio(LocalTime.of(18, 0));
        l.setOrarioFine(LocalTime.of(19, 0));
        l.setEmail("studente@example.invalid");
        l.setCodiceModifica(it.vasilepersonalsite.util.SimpleAES.cripta("ABCD1234", "test-chiave-aes-16"));
        return l;
    }

    private EmailServiceImpl email(MockRestServiceServer[] out) {
        EmailServiceImpl s = new EmailServiceImpl();
        ReflectionTestUtils.setField(s, "notificationTo", "admin@example.invalid");
        ReflectionTestUtils.setField(s, "apiKey", "k");
        ReflectionTestUtils.setField(s, "apiSecret", "s");
        ReflectionTestUtils.setField(s, "fromEmail", "noreply@example.invalid");
        ReflectionTestUtils.setField(s, "fromName", "T");
        ReflectionTestUtils.setField(s, "adminUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(s, "chiave", "test-chiave-aes-16");
        out[0] = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(s, "restTemplate")).build();
        return s;
    }

    // ------------------------------------------------------------------ Mailjet

    @Test
    void timeoutMailjetNonProvocaRetoNeEccezioni() {
        MockRestServiceServer[] srv = new MockRestServiceServer[1];
        EmailServiceImpl service = email(srv);
        // UNA sola richiesta attesa: una seconda farebbe fallire il test (AssertionError)
        srv[0].expect(requestTo(MAILJET)).andExpect(method(HttpMethod.POST))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertDoesNotThrow(() -> service.confermaLezione(lezione()));
        srv[0].verify();
    }

    // ------------------------------------------------------------------ Google

    private GoogleMeetServiceImpl google(MockRestServiceServer[] out) {
        GoogleMeetServiceImpl g = new GoogleMeetServiceImpl();
        ReflectionTestUtils.setField(g, "clientId", "id");
        ReflectionTestUtils.setField(g, "clientSecret", "secret");
        ReflectionTestUtils.setField(g, "refreshToken", "refresh");
        ReflectionTestUtils.setField(g, "calendarId", "primary");
        out[0] = MockRestServiceServer.bindTo((RestTemplate) ReflectionTestUtils.getField(g, "restTemplate")).build();
        return g;
    }

    @Test
    void timeoutGoogleSuCreazioneEventoNonRipeteLaRichiestaERestituisceNull() {
        MockRestServiceServer[] srv = new MockRestServiceServer[1];
        GoogleMeetServiceImpl g = google(srv);
        srv[0].expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"t\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        srv[0].expect(requestTo(EVENTS_URL))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertNull(g.creaLinkMeet(lezione()), "la conferma prosegue senza link");
        srv[0].verify(); // esattamente 1 token + 1 evento
    }

    @Test
    void timeoutGoogleSuTokenNonCreaEventi() {
        MockRestServiceServer[] srv = new MockRestServiceServer[1];
        GoogleMeetServiceImpl g = google(srv);
        srv[0].expect(requestTo(TOKEN_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertNull(g.creaLinkMeet(lezione()));
        srv[0].verify(); // nessuna richiesta di evento dopo il fallimento del token
    }

    @Test
    void requestIdMeetStabileTraDueTentativi() {
        MockRestServiceServer[] srv = new MockRestServiceServer[1];
        GoogleMeetServiceImpl g = google(srv);
        srv[0].expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"t\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        for (int i = 0; i < 2; i++) {
            srv[0].expect(requestTo(EVENTS_URL))
                    .andExpect(jsonPath("$.conferenceData.createRequest.requestId").value("lezione-xyz"))
                    .andRespond(i == 0 ? withException(new SocketTimeoutException("Read timed out"))
                            : withSuccess("{\"hangoutLink\":\"https://meet.google.com/aaa-bbbb-ccc\"}", MediaType.APPLICATION_JSON));
        }
        assertNull(g.creaLinkMeet(lezione()));
        assertEquals("https://meet.google.com/aaa-bbbb-ccc", g.creaLinkMeet(lezione()));
        srv[0].verify();
    }

    // ------------------------------------------------------------------ client HTTP reale

    /** Server che accetta e legge la richiesta ma non risponde mai: conta le richieste ricevute. */
    private int richiesteRicevuteDaServerMuto(boolean chiudiDopoLettura) throws Exception {
        AtomicInteger richieste = new AtomicInteger();
        List<Socket> aperte = new ArrayList<>();
        try (ServerSocket ss = new ServerSocket(0)) {
            Thread t = new Thread(() -> {
                try {
                    while (!ss.isClosed()) {
                        Socket s = ss.accept();
                        synchronized (aperte) { aperte.add(s); }
                        Thread h = new Thread(() -> {
                            try {
                                InputStream in = s.getInputStream();
                                byte[] buf = new byte[8192];
                                // conta una richiesta per ogni blocco che contiene una request-line POST
                                int n = in.read(buf);
                                if (n > 0 && new String(buf, 0, n).startsWith("POST")) richieste.incrementAndGet();
                                if (chiudiDopoLettura) s.close();
                                else while (in.read(buf) >= 0) { /* resta muto */ }
                            } catch (Exception ignored) { }
                        });
                        h.setDaemon(true);
                        h.start();
                    }
                } catch (Exception ignored) { }
            });
            t.setDaemon(true);
            t.start();

            SimpleClientHttpRequestFactory f = HttpTimeouts.requestFactory();
            f.setReadTimeout(700);
            assertThrows(ResourceAccessException.class, () ->
                    new RestTemplate(f).postForObject("http://127.0.0.1:" + ss.getLocalPort() + "/send", "{}", String.class));
            Thread.sleep(1500); // tempo per un eventuale retry tardivo
        } finally {
            synchronized (aperte) { for (Socket s : aperte) try { s.close(); } catch (Exception ignored) { } }
        }
        return richieste.get();
    }

    @Test
    void readTimeoutRealeNonReinviaIlPost() throws Exception {
        assertEquals(1, richiesteRicevuteDaServerMuto(false), "il POST deve arrivare UNA sola volta");
    }

    @Test
    void connessioneChiusaDalServerDopoLaRichiestaNonReinviaIlPost() throws Exception {
        assertEquals(1, richiesteRicevuteDaServerMuto(true), "il POST deve arrivare UNA sola volta");
    }
}
