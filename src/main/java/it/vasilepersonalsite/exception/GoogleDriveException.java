package it.vasilepersonalsite.exception;

/**
 * Errore nella comunicazione con Google Drive (token OAuth, upload, timeout).
 * Il messaggio è sintetico e non contiene mai token o credenziali.
 */
public class GoogleDriveException extends RuntimeException {

    private final int httpStatus;

    public GoogleDriveException(String message) {
        this(message, -1, null);
    }

    public GoogleDriveException(String message, Throwable cause) {
        this(message, -1, cause);
    }

    public GoogleDriveException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** Codice HTTP restituito da Google, -1 se l'errore è di rete o di timeout. */
    public int getHttpStatus() {
        return httpStatus;
    }
}
