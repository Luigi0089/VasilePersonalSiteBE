package it.vasilepersonalsite.exception.ErrorRespons;

import java.time.LocalTime;

public class FasciaOrariaDisponibile {

    private LocalTime orarioInizio;
    private LocalTime orarioFine;

    public FasciaOrariaDisponibile(LocalTime orarioInizio, LocalTime orarioFine) {
        this.orarioInizio = orarioInizio;
        this.orarioFine = orarioFine;
    }

    public LocalTime getOrarioInizio() {
        return orarioInizio;
    }

    public LocalTime getOrarioFine() {
        return orarioFine;
    }
}

