package it.vasilepersonalsite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurazione dello snapshot del calendario su Google Drive (prefisso {@code calendario.snapshot}).
 * Tutti i valori sensibili arrivano da variabili d'ambiente: nessun segreto nel codice.
 *
 * @param enabled             interruttore generale: false = nessuna chiamata a Drive, nessun effetto allo shutdown
 * @param publishOnShutdown   pubblica lo snapshot durante lo spegnimento dell'istanza
 * @param driveFileId         id del file JSON su Drive (sempre lo stesso, viene sovrascritto)
 * @param clientId            client OAuth (può coincidere con quello di Google Calendar)
 * @param clientSecret        secret del client OAuth
 * @param refreshToken        refresh token DEDICATO con il solo scope drive.file
 *                            (separato da quello di Calendar: revocarlo non tocca Calendar)
 * @param settimane           settimane coperte a partire da quella corrente (inclusa)
 * @param connectTimeoutMs    timeout di connessione verso Google
 * @param requestTimeoutMs    timeout di ogni singola richiesta HTTP verso Google
 * @param shutdownBudgetMs    tempo massimo complessivo concesso alla pubblicazione allo shutdown
 *                            (App Engine invia SIGKILL circa 2 s dopo SIGTERM)
 */
@ConfigurationProperties(prefix = "calendario.snapshot")
public record CalendarioSnapshotProperties(
        boolean enabled,
        boolean publishOnShutdown,
        String driveFileId,
        String clientId,
        String clientSecret,
        String refreshToken,
        int settimane,
        int connectTimeoutMs,
        int requestTimeoutMs,
        int shutdownBudgetMs
) {

    public CalendarioSnapshotProperties {
        driveFileId = blankToNull(driveFileId);
        clientId = blankToNull(clientId);
        clientSecret = blankToNull(clientSecret);
        refreshToken = blankToNull(refreshToken);
        settimane = settimane <= 0 ? 8 : Math.min(settimane, 26);
        connectTimeoutMs = connectTimeoutMs <= 0 ? 700 : connectTimeoutMs;
        requestTimeoutMs = requestTimeoutMs <= 0 ? 1500 : requestTimeoutMs;
        shutdownBudgetMs = shutdownBudgetMs <= 0 ? 1500 : shutdownBudgetMs;
    }

    /** Credenziali OAuth presenti (necessarie per qualsiasi chiamata a Drive). */
    public boolean credenzialiPresenti() {
        return clientId != null && clientSecret != null && refreshToken != null;
    }

    /** Pronto per pubblicare: abilitato, credenziali e id del file presenti. */
    public boolean pubblicazioneConfigurata() {
        return enabled && credenzialiPresenti() && driveFileId != null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
