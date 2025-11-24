package it.vasilepersonalsite.entity;


import io.swagger.v3.oas.annotations.media.Schema;
import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.constans.Stato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.apache.commons.text.RandomStringGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

// PrenotazioneLezione.java
@Entity
@Table(name = "lezione_prenotata")
@Data
public class PrenotazioneLezione {

    private static final RandomStringGenerator CODICE_GENERATOR =
            new RandomStringGenerator.Builder()
                    .withinRange('0', 'z')           // range ASCII
                    .filteredBy(Character::isLetterOrDigit) // solo lettere e numeri
                    .build();

    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "nome_studente")
    private String nomeStudente;

    @NotBlank
    @Column(name = "materia")
    private String materia;  // es: "Analisi 1", "Geometria", ecc.

    @Column(name = "livello")
    private String livello;  // es: "Scuole superiori", "Università"

    @Column(name = "note")
    private String note;

    @Column(name = "data_lezione", nullable = false)
    private LocalDate dataLezione;

    @Column(name = "orario_inizio", nullable = false)
    private LocalTime orarioInizio;

    @Column(name = "orario_fine", nullable = false)
    private LocalTime orarioFine;

    @Column(name = "stato", nullable = false)
    @Schema(description = "Stato della prenotazione",
            example = "CONFERMATA")
    private String stato = Stato.IN_APPROVAZIONE.getLabel();

    @Column(name = "data_creazione", nullable = false, updatable = false)
    @Schema(description = "Data di creazione della prenotazione",
            example = "2025-11-18T11:48:02.975Z")
    private Instant dataCreazione = Instant.now();

    @Column(name = "codice_modifica", nullable = false, unique = true)
    @Schema(description = "Codice univoco per modificare la prenotazione",
            example = "8df7e6ac-95f3-4f11-932b-df93e7c1a4e1")
    private String codiceModifica = generaCodiceModifica();

    @Column(name = "annullata", nullable = false)
    @Schema(description = "Indica se la lezione è stata annullata",
            example = "false")
    private boolean annullata = false;

    @Column(name = "mail", nullable = false)
    @Schema(description = "E-mail dello studente",
            example = "Pippo@gmail.com")
    private String email;


    public PrenotazioneLezione() {
    }

    public PrenotazioneLezione(LezioneRequestDto lezione) {

        this.nomeStudente = lezione.getNomeStudente();

        this.materia =  lezione.getMateria();

        this.livello = lezione.getLivello();

        this.note = lezione.getNote();

        this.dataLezione = lezione.getDataLezione();

        this.orarioInizio = lezione.getOrarioInizio();

        this.orarioFine = lezione.getOrarioFine();

        this.email = lezione.getEmail();

    }

    public static String generaCodiceModifica() {

        return CODICE_GENERATOR.generate(8).toUpperCase();
    }



}

