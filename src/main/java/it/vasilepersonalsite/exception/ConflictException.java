package it.vasilepersonalsite.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class ConflictException extends RuntimeException {

    private final LocalDate data;
    private final LocalTime inizio;
    private final LocalTime fine;

    public ConflictException(LocalDate  data, LocalTime inizio, LocalTime fine) {
        super(String.format(
                "Impossibile prenotare la lezione per il giorno %s dalle ore %s alle ore %s, in quanto va in conflitto con altre lezioni.",
                data, inizio, fine));
        this.data = data;
        this.inizio = inizio;
        this.fine = fine;
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
        this.data = null;
        this.inizio = null;
        this.fine = null;
    }

    public ConflictException(String message) {
        super(message);
        this.data = null;
        this.inizio = null;
        this.fine = null;
    }

    public LocalDate  getData() {
        return data;
    }

    public LocalTime getInizio() {
        return inizio;
    }

    public LocalTime getFine() {
        return fine;
    }
}
