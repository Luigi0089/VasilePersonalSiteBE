package it.vasilepersonalsite.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class LezioneResponseDto {


    @Schema(description = "id della prenotazione", example = "e2d358b4-8dec-4081-bc0d-00fc8ef5597c")
    private String id;

    @Schema(description = "Username dello studente", example = "Mario Rossi")
    private String nomeStudente;


    @Schema(description = "Materia della lezione", example = "Analisi 1")
    private String materia;  // es: "Analisi 1", "Geometria", ecc.

    @Schema(description = "Livello scolastico dello studente",
            example = "Università")
    private String livello;  // es: "Scuole superiori", "Università"

    @Schema(description = "Note aggiuntive inserite dallo studente",
            example = "Preferisco esercizi sui limiti")
    private String note;

    @Schema(description = "Data della lezione (ISO 8601)",
            example = "2025-12-03T00:00:00Z")
    private LocalDate dataLezione;

    @Schema(description = "Orario di inizio della lezione",
            example = "15:00:00")
    private LocalTime orarioInizio;

    @Schema(description = "Orario di fine della lezione",
            example = "16:30:00")
    private LocalTime orarioFine;

    @Schema(description = "Codice univoco per modificare la prenotazione",
            example = "QRT23SE4")
    private String codiceModifica ;

    @Schema(description = "Stato della prenotazione",
            example = "CONFERMATA")
    private String stato;

    @Schema(description = "E-mail dello studente",
            example = "Pippo@gmail.com")
    private String email;


    public LezioneResponseDto(PrenotazioneLezione lezione) {


        this.id = lezione.getId();

        this.nomeStudente = lezione.getNomeStudente();

        this.materia = lezione.getMateria();

        this.livello = lezione.getLivello();

        this.note = lezione.getNote();

        this.dataLezione = lezione.getDataLezione();

        this.orarioInizio = lezione.getOrarioInizio();

        this.orarioFine = lezione.getOrarioFine();

        this.codiceModifica = lezione.getCodiceModifica();

        this.stato = lezione.getStato();

        this.email = lezione.getEmail();
    }


}
