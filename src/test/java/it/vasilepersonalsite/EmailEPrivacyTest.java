package it.vasilepersonalsite;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.constans.Stato;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.service.impl.EmailServiceImpl;
import it.vasilepersonalsite.service.impl.LezioneServiceImpl;
import it.vasilepersonalsite.util.HttpTimeouts;
import it.vasilepersonalsite.util.LogPrivacy;
import it.vasilepersonalsite.util.SimpleAES;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Verifica, senza alcuna chiamata reale a Mailjet (MockRestServiceServer),
 * il payload delle email: logo inline, allegato .ics, HTML, e assenza di dati
 * personali completi nei log. Dati esclusivamente fittizi.
 */
class EmailEPrivacyTest {

    private static final String MAILJET = "https://api.mailjet.com/v3.1/send";
    private static final String CHIAVE = "test-chiave-aes-16";

    private EmailServiceImpl service;
    private MockRestServiceServer server;
    private final List<String> payload = new ArrayList<>();
    private ListAppender<ILoggingEvent> logs;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl();
        ReflectionTestUtils.setField(service, "notificationTo", "admin@example.invalid");
        ReflectionTestUtils.setField(service, "apiKey", "k");
        ReflectionTestUtils.setField(service, "apiSecret", "s");
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.invalid");
        ReflectionTestUtils.setField(service, "fromName", "Test");
        ReflectionTestUtils.setField(service, "adminUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(service, "chiave", CHIAVE);

        RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(rt).ignoreExpectOrder(true).build();

        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(EmailServiceImpl.class)).addAppender(logs);
        ((Logger) LoggerFactory.getLogger(LezioneServiceImpl.class)).addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(EmailServiceImpl.class)).detachAppender(logs);
        ((Logger) LoggerFactory.getLogger(LezioneServiceImpl.class)).detachAppender(logs);
    }

    private void attendiMailjet(int volte) {
        for (int i = 0; i < volte; i++) {
            server.expect(requestTo(MAILJET)).andExpect(method(HttpMethod.POST))
                    .andExpect(req -> payload.add(req.getBody().toString()))
                    .andRespond(withSuccess("{\"Messages\":[{\"Status\":\"success\",\"To\":[{\"Email\":\"studente@example.invalid\"}]}]}",
                            MediaType.APPLICATION_JSON));
        }
    }

    private PrenotazioneLezione lezioneConfermata() {
        PrenotazioneLezione l = new PrenotazioneLezione();
        l.setId("id-test-1");
        l.setNomeStudente("Studente Test");
        l.setMateria("Analisi 1");
        l.setLivello("Universita");
        l.setDataLezione(LocalDate.of(2026, 11, 23));
        l.setOrarioInizio(LocalTime.of(18, 0));
        l.setOrarioFine(LocalTime.of(19, 0));
        l.setEmail("studente@example.invalid");
        l.setStato(Stato.CONFERMATA.getLabel());
        l.setCodiceModifica(SimpleAES.cripta("ABCD1234", CHIAVE));
        l.setLinkMeet("https://meet.google.com/aaa-bbbb-ccc");
        return l;
    }

    private String testoLog() {
        StringBuilder sb = new StringBuilder();
        logs.list.forEach(e -> sb.append(e.getFormattedMessage()).append('\n'));
        return sb.toString();
    }

    @Test
    void confermaContieneLogoIcsELinkMeet() throws Exception {
        attendiMailjet(1);
        service.confermaLezione(lezioneConfermata());
        server.verify();

        JsonNode msg = json.readTree(payload.get(0)).get("Messages").get(0);
        assertEquals("studente@example.invalid", msg.get("To").get(0).get("Email").asText());
        String html = msg.get("HTMLPart").asText();
        assertTrue(html.contains("cid:logoImage"));
        assertTrue(html.contains("ABCD1234"), "codice di modifica decifrato nella mail");
        assertTrue(html.contains("https://meet.google.com/aaa-bbbb-ccc"));
        assertTrue(html.contains("max-height:60px"), "dimensione di visualizzazione invariata");

        JsonNode logo = msg.get("InlinedAttachments").get(0);
        assertEquals("logoImage", logo.get("ContentID").asText());
        assertEquals("image/png", logo.get("ContentType").asText());
        assertEquals("Logo.png", logo.get("Filename").asText());
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(
                Base64.getDecoder().decode(logo.get("Base64Content").asText())));
        assertEquals(180, img.getWidth());
        assertEquals(180, img.getHeight());

        JsonNode ics = msg.get("Attachments").get(0);
        assertEquals("text/calendar", ics.get("ContentType").asText());
        assertEquals("lezione.ics", ics.get("Filename").asText());
        String icsTesto = new String(Base64.getDecoder().decode(ics.get("Base64Content").asText()));
        assertTrue(icsTesto.startsWith("BEGIN:VCALENDAR"));
        assertTrue(icsTesto.contains("DTSTART:20261123T170000Z"), "18:00 ora italiana = 17:00 UTC");

        // payload molto più leggero: prima il solo logo valeva ~2,5 MB in base64
        assertTrue(payload.get(0).length() < 100_000, "payload " + payload.get(0).length() + " byte");
    }

    @Test
    void logoRiusatoTraPiuEmailESemprePresente() throws Exception {
        attendiMailjet(3);
        service.confermaLezione(lezioneConfermata());
        service.annullaLezione(lezioneConfermata());
        service.sendNotificaUtenzaBloccata("admin", 5, "ZXCV9876");
        server.verify();

        String primo = json.readTree(payload.get(0)).at("/Messages/0/InlinedAttachments/0/Base64Content").asText();
        for (String p : payload) {
            assertEquals(primo, json.readTree(p).at("/Messages/0/InlinedAttachments/0/Base64Content").asText());
        }
        assertTrue(json.readTree(payload.get(2)).at("/Messages/0/HTMLPart").asText().contains("ZXCV9876"));
    }

    @Test
    void notificaPrenotazioneStudenteEAdmin() throws Exception {
        attendiMailjet(2);
        LezioneResponseDto dto = new LezioneResponseDto(lezioneConfermata());
        dto.setCodiceModifica("ABCD1234");
        service.sendNotificaLezione(dto, "studente@example.invalid", false);
        server.verify();

        List<String> destinatari = new ArrayList<>();
        for (String p : payload) {
            destinatari.add(json.readTree(p).at("/Messages/0/To/0/Email").asText());
        }
        assertTrue(destinatari.containsAll(List.of("studente@example.invalid", "admin@example.invalid")));
    }

    @Test
    void logSenzaEmailCompleteNeBodyMailjet() {
        attendiMailjet(1);
        service.confermaLezione(lezioneConfermata());

        String log = testoLog();
        assertFalse(log.contains("studente@example.invalid"), log);
        assertTrue(log.contains("s***@example.invalid"), log);
        assertFalse(log.contains("\"Status\""), "il body della risposta Mailjet non va loggato");
    }

    @Test
    void mascheramentoEmail() {
        assertEquals("m***@gmail.com", LogPrivacy.email("mario.rossi@gmail.com"));
        assertEquals("***", LogPrivacy.email(null));
        assertEquals("***", LogPrivacy.email(""));
        assertEquals("***", LogPrivacy.email("senza-chiocciola"));
        assertEquals("***", LogPrivacy.email("@dominio.it"));
    }

    @Test
    void timeoutConfiguratiSuEntrambiIClient() {
        // istanze nuove: quella del setUp è già collegata a MockRestServiceServer
        for (Object client : List.of(new EmailServiceImpl(),
                new it.vasilepersonalsite.service.impl.GoogleMeetServiceImpl())) {
            RestTemplate rt = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
            assertInstanceOf(SimpleClientHttpRequestFactory.class, rt.getRequestFactory());
            SimpleClientHttpRequestFactory f = (SimpleClientHttpRequestFactory) rt.getRequestFactory();
            assertEquals(HttpTimeouts.CONNECT_TIMEOUT_MS, ReflectionTestUtils.getField(f, "connectTimeout"));
            assertEquals(HttpTimeouts.READ_TIMEOUT_MS, ReflectionTestUtils.getField(f, "readTimeout"));
        }
    }

    @Test
    void timeoutDiLetturaInterrompeUnServerBloccato() throws Exception {
        // server locale che accetta la connessione ma non risponde mai
        try (java.net.ServerSocket muto = new java.net.ServerSocket(0)) {
            AtomicReference<java.net.Socket> accettato = new AtomicReference<>();
            Thread t = new Thread(() -> {
                try { accettato.set(muto.accept()); } catch (Exception ignored) { }
            });
            t.setDaemon(true);
            t.start();

            SimpleClientHttpRequestFactory f = HttpTimeouts.requestFactory();
            f.setReadTimeout(500); // stessa logica, durata ridotta per il test
            long t0 = System.nanoTime();
            assertThrows(org.springframework.web.client.ResourceAccessException.class, () ->
                    new RestTemplate(f).getForObject("http://127.0.0.1:" + muto.getLocalPort() + "/", String.class));
            assertTrue((System.nanoTime() - t0) / 1_000_000 < 5_000);
            if (accettato.get() != null) accettato.get().close();
        }
    }

    /*
     * Concorrenza della cache del logo: N thread inviano email in contemporanea sulla stessa
     * istanza (come fanno i thread @Async). Tutti devono allegare lo STESSO logo, completo e
     * decodificabile; nessuna eccezione, nessun base64 parziale.
     */
    @Test
    void cacheLogoSicuraConInviiConcorrenti() throws Exception {
        int thread = 16;
        EmailServiceImpl s = new EmailServiceImpl();
        ReflectionTestUtils.setField(s, "notificationTo", "admin@example.invalid");
        ReflectionTestUtils.setField(s, "apiKey", "k");
        ReflectionTestUtils.setField(s, "apiSecret", "s");
        ReflectionTestUtils.setField(s, "fromEmail", "noreply@example.invalid");
        ReflectionTestUtils.setField(s, "fromName", "T");
        ReflectionTestUtils.setField(s, "adminUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(s, "chiave", CHIAVE);

        java.util.Queue<String> loghi = new java.util.concurrent.ConcurrentLinkedQueue<>();
        RestTemplate rt = new RestTemplate((uri, httpMethod) -> {
            var req = new org.springframework.mock.http.client.MockClientHttpRequest(httpMethod, uri) {
                @Override
                protected org.springframework.http.client.ClientHttpResponse executeInternal() throws java.io.IOException {
                    JsonNode body = json.readTree(getBodyAsString());
                    loghi.add(body.at("/Messages/0/InlinedAttachments/0/Base64Content").asText());
                    return new org.springframework.mock.http.client.MockClientHttpResponse("{}".getBytes(),
                            org.springframework.http.HttpStatus.OK);
                }
            };
            return req;
        });
        ReflectionTestUtils.setField(s, "restTemplate", rt);

        var pool = java.util.concurrent.Executors.newFixedThreadPool(thread);
        var start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<?>> f = new ArrayList<>();
        for (int i = 0; i < thread * 4; i++) {
            f.add(pool.submit(() -> { start.await(); s.sendNotificaUtenzaBloccata("admin", 3, "CODE1234"); return null; }));
        }
        start.countDown();
        for (var x : f) x.get(30, java.util.concurrent.TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(thread * 4, loghi.size(), "ogni email ha il logo");
        String atteso;
        try (var in = new ClassPathResource("static/Logo-email.png").getInputStream()) {
            atteso = Base64.getEncoder().encodeToString(in.readAllBytes());
        }
        for (String l : loghi) {
            assertEquals(atteso, l, "logo identico al file, mai parziale");
        }
    }

    /*
     * Qualità visiva: confronto pixel a pixel con l'originale, entrambi ridotti alla dimensione
     * a cui il client di posta mostra il logo (60px, 120px su retina 2x, 180px su 3x).
     * PSNR >= 40 dB = differenza non percepibile; anche il colore medio deve coincidere.
     */
    @Test
    void qualitaVisivaLogoEquivalenteAllOriginale() throws Exception {
        BufferedImage originale = ImageIO.read(new ClassPathResource("static/Logo.png").getInputStream());
        BufferedImage email = ImageIO.read(new ClassPathResource("static/Logo-email.png").getInputStream());
        assertEquals(originale.getWidth() / (double) originale.getHeight(),
                email.getWidth() / (double) email.getHeight(), 1e-9, "proporzioni invariate");
        for (int lato : new int[]{60, 120, 180}) {
            double psnr = psnr(scala(originale, lato), scala(email, lato));
            assertTrue(psnr >= 40, lato + "px: PSNR " + psnr);
        }
        double[] mo = colorMedio(originale), me = colorMedio(email);
        for (int c = 0; c < 3; c++) assertEquals(mo[c], me[c], 2.0, "colore medio canale " + c);
    }

    /*
     * Riduzione per media d'area (riferimento per il downscale): con algoritmi diversi
     * (es. dimezzamenti + bilineare) lo stesso originale differisce da sé stesso di ~34 dB a 60px,
     * quindi il confronto sarebbe dominato dal rumore del metodo e non dal logo.
     */
    private static BufferedImage scala(BufferedImage src, int lato) {
        java.awt.Image s = src.getScaledInstance(lato, lato, java.awt.Image.SCALE_AREA_AVERAGING);
        BufferedImage out = new BufferedImage(lato, lato, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.drawImage(s, 0, 0, null);
        g.dispose();
        return out;
    }

    private static double psnr(BufferedImage a, BufferedImage b) {
        double se = 0;
        int n = 0;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                for (int sh = 0; sh <= 16; sh += 8) {
                    int d = ((p >> sh) & 0xff) - ((q >> sh) & 0xff);
                    se += d * d;
                    n++;
                }
            }
        double mse = se / n;
        return mse == 0 ? 99 : 10 * Math.log10(255.0 * 255.0 / mse);
    }

    private static double[] colorMedio(BufferedImage img) {
        double[] s = new double[3];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                s[0] += (p >> 16) & 0xff; s[1] += (p >> 8) & 0xff; s[2] += p & 0xff;
            }
        int n = img.getWidth() * img.getHeight();
        return new double[]{s[0] / n, s[1] / n, s[2] / n};
    }

    @Test
    void logoEmailEsisteEOriginaleInvariato() throws Exception {
        BufferedImage email = ImageIO.read(new ClassPathResource("static/Logo-email.png").getInputStream());
        BufferedImage originale = ImageIO.read(new ClassPathResource("static/Logo.png").getInputStream());
        assertEquals(180, email.getWidth());
        assertEquals(1024, originale.getWidth(), "Logo.png originale non modificato");
    }
}
