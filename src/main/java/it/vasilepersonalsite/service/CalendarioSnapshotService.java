package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.CalendarioSnapshotDto;
import it.vasilepersonalsite.DTO.CalendarioSnapshotEsitoDto;

import java.time.Duration;

/**
 * Snapshot pubblico (non autoritativo) del calendario, pubblicato su Google Drive.
 */
public interface CalendarioSnapshotService {

    /** Genera lo snapshot leggendo Neon (richiede il database disponibile). */
    CalendarioSnapshotDto genera();

    /** Genera e pubblica sul file configurato. Usato dall'endpoint admin. */
    CalendarioSnapshotEsitoDto pubblica(Duration timeout);

    /** Prima inizializzazione: crea il file pubblico su Drive e ne restituisce l'id. */
    CalendarioSnapshotEsitoDto inizializza(Duration timeout);
}
