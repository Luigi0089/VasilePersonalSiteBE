package it.vasilepersonalsite.exception;

public class UtenzaBloccataException extends RuntimeException {

    public UtenzaBloccataException(String username) {
        super("Utenza " + username + " bloccata per troppi tentativi di login falliti");
    }
}
