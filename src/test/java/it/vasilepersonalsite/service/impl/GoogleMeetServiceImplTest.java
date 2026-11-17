package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.entity.PrenotazioneLezione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleMeetServiceImplTest {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL =
            "https://www.googleapis.com/calendar/v3/calendars/primary/events"
                    + "?conferenceDataVersion=1&sendUpdates=none";

    private GoogleMeetServiceImpl service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new GoogleMeetServiceImpl();
        ReflectionTestUtils.setField(service, "clientId", "id");
        ReflectionTestUtils.setField(service, "clientSecret", "secret");
        ReflectionTestUtils.setField(service, "refreshToken", "refresh");
        ReflectionTestUtils.setField(service, "calendarId", "primary");

        RestTemplate restTemplate = new RestTemplate();
        service.restTemplate = restTemplate;
        server = MockRestServiceServer.createServer(restTemplate);
    }

    private PrenotazioneLezione lezione() {
        PrenotazioneLezione l = new PrenotazioneLezione();
        l.setId("abc-123");
        l.setNomeStudente("Mario Rossi");
        l.setMateria("Analisi 1");
        l.setLivello("Universitario");
        l.setDataLezione(LocalDate.of(2026, 11, 20));
        l.setOrarioInizio(LocalTime.of(18, 0));
        l.setOrarioFine(LocalTime.of(19, 0));
        return l;
    }

    private void attendiToken() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("refresh_token=refresh")))
                .andRespond(withSuccess("{\"access_token\":\"tok\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void creaEventoConMeetERestituisceIlLink() {
        attendiToken();
        server.expect(requestTo(EVENTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok"))
                .andExpect(jsonPath("$.start.dateTime").value("2026-11-20T18:00:00"))
                .andExpect(jsonPath("$.start.timeZone").value("Europe/Rome"))
                .andExpect(jsonPath("$.end.dateTime").value("2026-11-20T19:00:00"))
                .andExpect(jsonPath("$.conferenceData.createRequest.requestId").value("abc-123"))
                .andExpect(jsonPath("$.conferenceData.createRequest.conferenceSolutionKey.type")
                        .value("hangoutsMeet"))
                .andRespond(withSuccess("{\"hangoutLink\":\"https://meet.google.com/abc-defg-hij\"}",
                        MediaType.APPLICATION_JSON));

        assertEquals("https://meet.google.com/abc-defg-hij", service.creaLinkMeet(lezione()));
        server.verify();
    }

    @Test
    void usaIlPuntoDiAccessoVideoSeMancaHangoutLink() {
        attendiToken();
        server.expect(requestTo(EVENTS_URL))
                .andRespond(withSuccess("""
                        {"conferenceData":{"entryPoints":[
                          {"entryPointType":"phone","uri":"tel:+39000"},
                          {"entryPointType":"video","uri":"https://meet.google.com/xyz-xyzx-xyz"}
                        ]}}""", MediaType.APPLICATION_JSON));

        assertEquals("https://meet.google.com/xyz-xyzx-xyz", service.creaLinkMeet(lezione()));
    }

    @Test
    void erroreDiGoogleNonBloccaLaConferma() {
        attendiToken();
        server.expect(requestTo(EVENTS_URL)).andRespond(withServerError());

        assertNull(service.creaLinkMeet(lezione()));
    }

    @Test
    void senzaCredenzialiNonChiamaGoogle() {
        ReflectionTestUtils.setField(service, "refreshToken", "");

        assertNull(service.creaLinkMeet(lezione()));
        server.verify(); // nessuna richiesta attesa né ricevuta
    }

    @Test
    void accessTokenRiutilizzatoTraDueRichieste() {
        attendiToken(); // una sola richiesta di token per due eventi
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo(EVENTS_URL))
                    .andRespond(withSuccess("{\"hangoutLink\":\"https://meet.google.com/aaa-bbbb-ccc\"}",
                            MediaType.APPLICATION_JSON));
        }

        assertEquals("https://meet.google.com/aaa-bbbb-ccc", service.creaLinkMeet(lezione()));
        assertEquals("https://meet.google.com/aaa-bbbb-ccc", service.creaLinkMeet(lezione()));
        server.verify();
    }
}
