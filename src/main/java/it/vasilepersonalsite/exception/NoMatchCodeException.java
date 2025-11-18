package it.vasilepersonalsite.exception;

public class NoMatchCodeException extends RuntimeException {

    public NoMatchCodeException(String id, String codice) {
        super("Codice modifica: "+ codice +" non valido per la lezione con ID " + id);
    }

    public NoMatchCodeException(String message) {
        super(message);
    }
}
