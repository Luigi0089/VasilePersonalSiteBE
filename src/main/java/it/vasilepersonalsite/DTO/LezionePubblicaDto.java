package it.vasilepersonalsite.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Vista pubblica di una lezione, usata dal calendario delle prenotazioni.
 * Non contiene dati personali dello studente (nome, email, note)
 * né il codice di modifica.
 */
@Data
@NoArgsConstructor
public class LezionePubblicaDto {

    @Schema(description = "id della prenotazione, necessario per modifica/annullamento tramite codice",
            example = "e2d358b4-8dec-4081-bc0d-00fc8ef5597c")
    private String id;

    @Schema(description = "Materia della lezione", example = "Analisi 1")
    private String materia;

    @Schema(description = "Livello scolastico", example = "Università")
    private String livello;

    @Schema(description = "Data della lezione", example = "2025-12-03")
    private LocalDate dataLezione;

    @Schema(description = "Orario di inizio", example = "18:00:00")
    private LocalTime orarioInizio;

    @Schema(description = "Orario di fine", example = "19:00:00")
    private LocalTime orarioFine;

    @Schema(description = "Stato della prenotazione", example = "CONFERMATA")
    private String stato;

    public LezionePubblicaDto(PrenotazioneLezione lezione) {
        this.id = lezione.getId();
        this.materia = lezione.getMateria();
        this.livello = lezione.getLivello();
        this.dataLezione = lezione.getDataLezione();
        this.orarioInizio = lezione.getOrarioInizio();
        this.orarioFine = lezione.getOrarioFine();
        this.stato = lezione.getStato();
    }
}
