package it.vasilepersonalsite.DTO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Fotografia pubblica del calendario delle lezioni, pubblicata su Google Drive e letta
 * dal frontend mentre il backend è spento o in avvio. NON è una fonte autoritativa:
 * quando il backend risponde, prevalgono sempre i dati di GET /lezioni/settimana.
 * Contiene solo intervalli occupati e il loro stato: nessun nome, email, nota, materia,
 * livello, codice di modifica o identificativo della prenotazione.
 * @param schemaVersion versione del formato (il frontend scarta versioni sconosciute)
 * @param generatedAt   istante di generazione (UTC)
 * @param timezone      fuso in cui sono espresse date e orari ("Europe/Rome")
 * @param coperturaDal  primo giorno coperto (lunedì della settimana di generazione)
 * @param coperturaAl   ultimo giorno coperto (sabato): fuori intervallo la disponibilità è sconosciuta
 * @param occupati      lezioni non annullate nell'intervallo, ordinate per data e orario
 */
public record CalendarioSnapshotDto(
        int schemaVersion,
        Instant generatedAt,
        String timezone,
        LocalDate coperturaDal,
        LocalDate coperturaAl,
        List<Occupato> occupati
) {

    /**
     * Intervallo occupato. {@code stato} usa le stesse etichette dell'API pubblica
     * ("Confermata", "In approvazione") per riutilizzare la logica del frontend.
     */
    public record Occupato(LocalDate data, LocalTime inizio, LocalTime fine, String stato) {
    }
}
