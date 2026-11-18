package it.vasilepersonalsite.DTO;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Esito delle operazioni amministrative sullo snapshot del calendario.
 * Non contiene credenziali né il contenuto dello snapshot.
 */
public record CalendarioSnapshotEsitoDto(
        Esito esito,
        String messaggio,
        Instant generatedAt,
        LocalDate coperturaDal,
        LocalDate coperturaAl,
        Integer lezioniOccupate,
        String fileId
) {

    public enum Esito {
        /** Contenuto del file Drive aggiornato. */
        PUBBLICATO,
        /** File Drive creato (prima inizializzazione). */
        CREATO,
        /** Funzionalità disattivata da configurazione. */
        DISABILITATO,
        /** Credenziali OAuth o id del file mancanti. */
        NON_CONFIGURATO,
        /** Inizializzazione richiesta ma il file è già configurato. */
        GIA_CONFIGURATO,
        /** Google Drive ha rifiutato o non ha completato l'operazione. */
        ERRORE_DRIVE
    }
}
