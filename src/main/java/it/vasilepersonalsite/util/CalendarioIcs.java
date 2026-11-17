package it.vasilepersonalsite.util;

import it.vasilepersonalsite.entity.PrenotazioneLezione;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Genera un file iCalendar (.ics, RFC 5545) per una lezione.
 * Allegato alla mail di conferma, permette al telefono / client di posta
 * di riconoscere data e ora e aggiungere l'evento al calendario.
 */
public final class CalendarioIcs {

    /** Le lezioni sono sempre espresse nell'ora italiana. */
    private static final ZoneId ZONA_LEZIONI = ZoneId.of("Europe/Rome");

    private static final DateTimeFormatter FORMATO_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private static final String CRLF = "\r\n";

    /** Lunghezza massima di una riga in ottetti (RFC 5545 §3.1). */
    private static final int MAX_OTTETTI_RIGA = 75;

    private CalendarioIcs() {
    }

    public static String perLezione(PrenotazioneLezione lezione) {
        String titolo = (lezione.isLezioneDiProva() ? "Lezione di prova di " : "Lezione di ")
                + lezione.getMateria();

        String descrizione = "Materia: " + lezione.getMateria()
                + "\nLivello: " + lezione.getLivello()
                + "\nStato: " + lezione.getStato();

        if (lezione.getLinkMeet() != null && !lezione.getLinkMeet().isBlank()) {
            descrizione += "\nGoogle Meet: " + lezione.getLinkMeet();
        }

        StringBuilder ics = new StringBuilder();

        riga(ics, "BEGIN:VCALENDAR");
        riga(ics, "VERSION:2.0");
        riga(ics, "PRODID:-//Luigi Vasile//Lezioni private//IT");
        riga(ics, "CALSCALE:GREGORIAN");
        // PUBLISH: evento da aggiungere al calendario, non un invito da accettare
        riga(ics, "METHOD:PUBLISH");

        riga(ics, "BEGIN:VEVENT");
        // UID stabile: se la lezione viene riconfermata, il calendario aggiorna l'evento invece di duplicarlo
        riga(ics, "UID:" + lezione.getId() + "@vasilepersonalsite");
        riga(ics, "DTSTAMP:" + FORMATO_UTC.format(Instant.now()));
        riga(ics, "DTSTART:" + utc(lezione.getDataLezione(), lezione.getOrarioInizio()));
        riga(ics, "DTEND:" + utc(lezione.getDataLezione(), lezione.getOrarioFine()));
        riga(ics, "SUMMARY:" + escape(titolo));
        riga(ics, "DESCRIPTION:" + escape(descrizione));
        if (lezione.getLinkMeet() != null && !lezione.getLinkMeet().isBlank()) {
            // i client di calendario mostrano il link come "Partecipa" / luogo dell'evento
            riga(ics, "LOCATION:" + escape(lezione.getLinkMeet()));
            riga(ics, "URL:" + lezione.getLinkMeet());
        }
        riga(ics, "STATUS:CONFIRMED");
        riga(ics, "TRANSP:OPAQUE");

        // promemoria un'ora prima
        riga(ics, "BEGIN:VALARM");
        riga(ics, "ACTION:DISPLAY");
        riga(ics, "DESCRIPTION:" + escape("Promemoria: " + titolo));
        riga(ics, "TRIGGER:-PT1H");
        riga(ics, "END:VALARM");

        riga(ics, "END:VEVENT");
        riga(ics, "END:VCALENDAR");

        return ics.toString();
    }

    /* Ora locale italiana → istante UTC (gestisce da solo ora legale/solare). */
    private static String utc(LocalDate data, LocalTime ora) {
        return FORMATO_UTC.format(data.atTime(ora).atZone(ZONA_LEZIONI).toInstant());
    }

    /* Escape dei caratteri speciali nei campi TEXT (RFC 5545 §3.3.11). */
    private static String escape(String testo) {
        if (testo == null) {
            return "";
        }
        return testo
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    /*
     * Aggiunge una riga, spezzandola ("folding") se supera 75 ottetti UTF-8:
     * le righe di continuazione iniziano con uno spazio.
     */
    private static void riga(StringBuilder ics, String contenuto) {
        int ottettiRiga = 0;

        for (int i = 0; i < contenuto.length(); ) {
            int codePoint = contenuto.codePointAt(i);
            String carattere = new String(Character.toChars(codePoint));
            int ottetti = carattere.getBytes(StandardCharsets.UTF_8).length;

            if (ottettiRiga + ottetti > MAX_OTTETTI_RIGA) {
                ics.append(CRLF).append(' ');
                ottettiRiga = 1; // lo spazio iniziale conta
            }

            ics.append(carattere);
            ottettiRiga += ottetti;
            i += Character.charCount(codePoint);
        }

        ics.append(CRLF);
    }
}
