package it.vasilepersonalsite.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Accesso minimo a Google Drive per lo snapshot pubblico del calendario.
 * Opera solo su file creati da questa applicazione (scope OAuth {@code drive.file}).
 */
public interface GoogleDriveService {

    /**
     * Sovrascrive il contenuto del file indicato (sempre lo stesso file, nessuna copia).
     * Drive sostituisce il contenuto solo a upload completato: in caso di errore o
     * timeout resta il contenuto precedente.
     *
     * @param scadenza istante oltre il quale la richiesta viene interrotta
     */
    void aggiornaContenuto(String fileId, byte[] json, Instant scadenza);

    /**
     * Crea il file JSON (prima inizializzazione), lo rende leggibile pubblicamente in sola
     * lettura ("chiunque abbia il link") e restituisce l'id da configurare.
     */
    String creaFilePubblico(String nomeFile, byte[] json, Duration timeout);
}
