package it.vasilepersonalsite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.LezioneService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.LESSON_PATH)
public class LezioniController {

    @Autowired
    private LezioneService lezioniService;


    /**
     * CREA una nuova lezione
     */
    @Operation(
            summary = "Crea una nuova lezione",
            description = "Inserisce una nuova prenotazione nel sistema"
    )
    @PostMapping("")
    public ResponseEntity<LezioneResponseDto> creaLezione(@Valid @RequestBody LezioneRequestDto lezione) {
        LezioneResponseDto lezioneResponse = lezioniService.creaLezione(lezione);
        return ResponseEntity.ok(lezioneResponse);
    }

    /**
     * MODIFICA una lezione esistente
     */
    @Operation(
            summary = "Modifica una lezione esistente",
            description = "Aggiorna i dati di una lezione salvata"
    )
    @PutMapping("/modifica")
    public ResponseEntity<LezioneResponseDto> modificaLezione(@Valid @RequestBody LezioneRequestDto lezioneAggiornata) {

        LezioneResponseDto lezioneResponse = lezioniService.modificaLezione(lezioneAggiornata);

        return ResponseEntity.ok(lezioneResponse);
    }

    /**
     * ANNULLA una lezione esistente
     */
    @Operation(
            summary = "Annulla una lezione",
            description = "Segna come annullata una lezione prenotata"
    )
    @PutMapping("/annulla")
    public ResponseEntity<LezioneResponseDto> annullaLezione(@Valid @RequestBody LezioneRequestDto lezioneAggiornata) {

        LezioneResponseDto lezioneResponse = lezioniService.annullaLezione(lezioneAggiornata);

        return ResponseEntity.ok(lezioneResponse);
    }


    @GetMapping(value = "/settimana", produces = "application/json")
    public ResponseEntity<List<LezioneResponseDto>> getLezioniSettimana(
            @Parameter(
                    description = "Una data qualsiasi della settimana desiderata (formato ISO: yyyy-MM-dd)",
                    example = "2025-12-04"
            )
            @RequestParam("data")
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate data
    ) {
        List<LezioneResponseDto> lezioni = lezioniService.trovaLezioniSettimana(data);
        return ResponseEntity.ok(lezioni);
    }


}

