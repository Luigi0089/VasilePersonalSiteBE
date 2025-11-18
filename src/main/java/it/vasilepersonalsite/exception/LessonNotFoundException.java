package it.vasilepersonalsite.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class LessonNotFoundException extends RuntimeException {


    public LessonNotFoundException() {}

    public LessonNotFoundException(String message) {
        super(message);
    }

    public LessonNotFoundException(String nome, LocalDate data, LocalTime inizio, LocalTime fine) {
        super("Lezione di "+ nome + "del " + data + " dalle ore: " + inizio + " alle ore: " + fine + " non trovata");
    }
}

