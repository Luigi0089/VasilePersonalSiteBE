package it.vasilepersonalsite.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LezioneRequestDto {

    @Schema(description = "id della prenotazione", example = "e2d358b4-8dec-4081-bc0d-00fc8ef5597c")
    private String id;

    @NotBlank
    @NotNull
    @Schema(description = "Username dello studente", example = "Mario Rossi")
    private String nomeStudente;

    @NotBlank
    @NotNull
    @Schema(description = "Materia della lezione", example = "Analisi 1")
    private String materia;  // es: "Analisi 1", "Geometria", ecc.

    @NotBlank
    @NotNull
    @Schema(description = "Livello scolastico dello studente",
            example = "Università")
    private String livello;  // es: "Scuole superiori", "Università"

    @Schema(description = "Note aggiuntive inserite dallo studente",
            example = "Preferisco esercizi sui limiti")
    private String note;

    @NotBlank
    @NotNull
    @Schema(description = "Data della lezione (ISO 8601)",
            example = "2025-12-03T00:00:00Z")
    private LocalDate dataLezione;

    @NotBlank
    @NotNull
    @Schema(description = "Orario di inizio della lezione",
            example = "15:00:00")
    private LocalTime orarioInizio;

    @NotBlank
    @NotNull
    @Schema(description = "Orario di fine della lezione",
            example = "16:30:00")
    private LocalTime orarioFine;

    @Schema(description = "Codice univoco per modificare la prenotazione",
            example = "QRT23SE4")
    private String codiceModifica ;



}
