package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.service.GoogleMeetService;
import it.vasilepersonalsite.util.HttpTimeouts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Genera i link Google Meet creando un evento su Google Calendar con
 * {@code conferenceData} (tipo hangoutsMeet).
 * Autenticazione: OAuth2 con refresh token dell'account Google che ospita le lezioni
 * (scope https://www.googleapis.com/auth/calendar.events).
 */
@Slf4j
@Service
public class GoogleMeetServiceImpl implements GoogleMeetService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private static final String EVENTS_URL =
            "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events"
                    + "?conferenceDataVersion=1&sendUpdates=none";

    /** Le lezioni sono sempre espresse nell'ora italiana. */
    private static final String FUSO_LEZIONI = "Europe/Rome";

    @Value("${google.calendar.client-id:}")
    private String clientId;

    @Value("${google.calendar.client-secret:}")
    private String clientSecret;

    @Value("${google.calendar.refresh-token:}")
    private String refreshToken;

    @Value("${google.calendar.calendar-id:primary}")
    private String calendarId;

    // non final: nei test viene sostituito con uno collegato a MockRestServiceServer
    // timeout espliciti: la creazione del link avviene durante la conferma admin (chiamata sincrona)
    RestTemplate restTemplate = new RestTemplate(HttpTimeouts.requestFactory());

    private String accessTokenInCache;
    private Instant scadenzaAccessToken = Instant.EPOCH;

    @Override
    public String creaLinkMeet(PrenotazioneLezione lezione) {

        if (!configurato()) {
            log.warn("GOOGLE MEET → credenziali Google non configurate, link non generato");
            return null;
        }

        try {
            Map<String, Object> evento = costruisciEvento(lezione);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken());

            @SuppressWarnings("unchecked")
            Map<String, Object> risposta = restTemplate.postForObject(
                    EVENTS_URL,
                    new HttpEntity<>(evento, headers),
                    Map.class,
                    calendarId
            );

            String link = estraiLink(risposta);

            if (link == null) {
                log.error("GOOGLE MEET → evento creato ma senza link Meet per la lezione {}: {}",
                        lezione.getId(), risposta);
            } else {
                log.info("GOOGLE MEET → link creato per la lezione {}", lezione.getId());
            }
            return link;

        } catch (Exception e) {
            log.error("GOOGLE MEET → errore nella creazione del link per la lezione {}",
                    lezione.getId(), e);
            return null;
        }
    }

    private boolean configurato() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }

    private Map<String, Object> costruisciEvento(PrenotazioneLezione lezione) {
        String titolo = (lezione.isLezioneDiProva() ? "Lezione di prova di " : "Lezione di ")
                + lezione.getMateria()
                + " - " + lezione.getNomeStudente();

        String descrizione = "Studente: " + lezione.getNomeStudente()
                + "\nMateria: " + lezione.getMateria()
                + "\nLivello: " + lezione.getLivello();

        return Map.of(
                "summary", titolo,
                "description", descrizione,
                "start", orario(lezione.getDataLezione(), lezione.getOrarioInizio()),
                "end", orario(lezione.getDataLezione(), lezione.getOrarioFine()),
                "conferenceData", Map.of(
                        "createRequest", Map.of(
                                // stesso id → Google non crea una seconda conferenza se la richiesta si ripete
                                "requestId", lezione.getId(),
                                "conferenceSolutionKey", Map.of("type", "hangoutsMeet")
                        )
                ),
                "reminders", Map.of(
                        "useDefault", false,
                        "overrides", List.of(
                                Map.of(
                                        "method", "popup",
                                        "minutes", 15
                                )
                        )
                )
        );
    }

    private static Map<String, String> orario(LocalDate data, LocalTime ora) {
        return Map.of(
                "dateTime", data.atTime(ora).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "timeZone", FUSO_LEZIONI
        );
    }

    @SuppressWarnings("unchecked")
    private static String estraiLink(Map<String, Object> evento) {
        if (evento == null) {
            return null;
        }

        Object hangoutLink = evento.get("hangoutLink");
        if (hangoutLink instanceof String link && !link.isBlank()) {
            return link;
        }

        // fallback: punto di accesso video della conferenza
        Object conferenza = evento.get("conferenceData");
        if (conferenza instanceof Map<?, ?> conferenzaMap
                && conferenzaMap.get("entryPoints") instanceof List<?> entryPoints) {
            for (Object entryPoint : entryPoints) {
                if (entryPoint instanceof Map<?, ?> punto
                        && "video".equals(punto.get("entryPointType"))
                        && punto.get("uri") instanceof String uri
                        && !uri.isBlank()) {
                    return uri;
                }
            }
        }
        return null;
    }

    /* Access token OAuth2 ottenuto dal refresh token, riusato finché non sta per scadere. */
    private synchronized String accessToken() {
        if (accessTokenInCache != null && Instant.now().isBefore(scadenzaAccessToken)) {
            return accessTokenInCache;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        @SuppressWarnings("unchecked")
        Map<String, Object> risposta = restTemplate.postForObject(
                TOKEN_URL,
                new HttpEntity<>(form, headers),
                Map.class
        );

        if (risposta == null || !(risposta.get("access_token") instanceof String token)) {
            throw new IllegalStateException("Google non ha restituito un access token");
        }

        long scadeTraSecondi = risposta.get("expires_in") instanceof Number n ? n.longValue() : 3600L;

        accessTokenInCache = token;
        // un minuto di margine per non usare un token appena scaduto
        scadenzaAccessToken = Instant.now().plusSeconds(Math.max(scadeTraSecondi - 60, 0));

        return accessTokenInCache;
    }
}
