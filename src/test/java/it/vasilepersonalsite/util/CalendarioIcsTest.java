package it.vasilepersonalsite.util;

import it.vasilepersonalsite.entity.PrenotazioneLezione;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarioIcsTest {

    private PrenotazioneLezione lezione(LocalDate data, String materia) {
        PrenotazioneLezione l = new PrenotazioneLezione();
        l.setId("abc-123");
        l.setMateria(materia);
        l.setLivello("Universitario");
        l.setStato("Confermata");
        l.setDataLezione(data);
        l.setOrarioInizio(LocalTime.of(18, 0));
        l.setOrarioFine(LocalTime.of(19, 0));
        return l;
    }

    @Test
    void convertiOraItalianaInUtc_oraSolare() {
        // novembre: CET = UTC+1 → 18:00 locali = 17:00Z
        String ics = CalendarioIcs.perLezione(lezione(LocalDate.of(2026, 11, 20), "Analisi 1"));

        assertTrue(ics.contains("DTSTART:20261120T170000Z\r\n"), ics);
        assertTrue(ics.contains("DTEND:20261120T180000Z\r\n"), ics);
        assertTrue(ics.contains("UID:abc-123@vasilepersonalsite\r\n"));
        assertTrue(ics.contains("TRIGGER:-PT1H\r\n"));
    }

    @Test
    void convertiOraItalianaInUtc_oraLegale() {
        // luglio: CEST = UTC+2 → 18:00 locali = 16:00Z
        String ics = CalendarioIcs.perLezione(lezione(LocalDate.of(2026, 7, 10), "Analisi 1"));

        assertTrue(ics.contains("DTSTART:20260710T160000Z\r\n"), ics);
    }

    @Test
    void escapeEFoldingDelleRigheLunghe() {
        String materia = "Analisi, Geometria; Algebra lineare e altre materie con nome molto lungo àèìòù";
        String ics = CalendarioIcs.perLezione(lezione(LocalDate.of(2026, 11, 20), materia));

        assertTrue(ics.contains("\\,"));
        assertTrue(ics.contains("\\;"));

        for (String riga : ics.split("\r\n")) {
            assertTrue(riga.getBytes(StandardCharsets.UTF_8).length <= 75, "riga troppo lunga: " + riga);
        }

        // unfolding (RFC 5545): rimuovendo CRLF+spazio si ricostruisce il titolo originale
        String unfolded = ics.replace("\r\n ", "");
        assertTrue(unfolded.contains("SUMMARY:Lezione di Analisi\\, Geometria\\; Algebra lineare"));
        assertFalse(unfolded.contains("\r\n "));
        assertEquals("BEGIN:VCALENDAR", ics.split("\r\n")[0]);
    }

    @Test
    void includeIlLinkMeetSoloSeDisponibile() {
        PrenotazioneLezione l = lezione(LocalDate.of(2026, 11, 20), "Analisi 1");
        assertFalse(CalendarioIcs.perLezione(l).contains("LOCATION:"));

        l.setLinkMeet("https://meet.google.com/abc-defg-hij");
        String unfolded = CalendarioIcs.perLezione(l).replace("\r\n ", "");

        assertTrue(unfolded.contains("LOCATION:https://meet.google.com/abc-defg-hij\r\n"));
        assertTrue(unfolded.contains("URL:https://meet.google.com/abc-defg-hij\r\n"));
        assertTrue(unfolded.contains("Google Meet: https://meet.google.com/abc-defg-hij"));
    }
}
