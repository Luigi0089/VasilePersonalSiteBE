package it.vasilepersonalsite.exception;

import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.exception.ErrorRespons.FasciaOrariaDisponibile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@ControllerAdvice
public class ExceptionController {

    @Autowired
    private LezioneDao lezioneDao;
    // -------------------------
    // 404 - Lezione non trovata
    // -------------------------
    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLessonNotFound(LessonNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // -------------------------
    // 400 - Codice modifica errato
    // -------------------------
    @ExceptionHandler(NoMatchCodeException.class)
    public ResponseEntity<Map<String, Object>> handleNoMatchCode(NoMatchCodeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // -------------------------
    // 409 - Conflitti (orari sovrapposti, ecc.)
    // -------------------------
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {

        List<FasciaOrariaDisponibile> fasceDisponibili =
                calcolaFasceDisponibiliPerGiorno(ex.getData());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("dataLezione", ex.getData());
        body.put("richiestaInizio", ex.getInizio());
        body.put("richiestaFine", ex.getFine());
        body.put("fasceDisponibili", fasceDisponibili);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private List<FasciaOrariaDisponibile> calcolaFasceDisponibiliPerGiorno(LocalDate data) {
        // orari di lavoro "standard" (puoi cambiarli)
        LocalTime inizioGiornata = LocalTime.of(18, 0);
        LocalTime fineGiornata   = LocalTime.of(21, 0);

        // tutte le lezioni non annullate di quel giorno, ordinate per orario di inizio
        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneAndAnnullataFalseOrderByOrarioInizioAsc(data);

        List<FasciaOrariaDisponibile> fasce = new ArrayList<>();

        LocalTime corrente = inizioGiornata;

        for (PrenotazioneLezione l : lezioni) {
            // se c'è "buco" tra corrente e inizio della lezione, è una fascia disponibile
            if (corrente.isBefore(l.getOrarioInizio())) {
                fasce.add(new FasciaOrariaDisponibile(corrente, l.getOrarioInizio()));
            }
            // aggiorno il puntatore a fine della lezione se va oltre
            if (l.getOrarioFine().isAfter(corrente)) {
                corrente = l.getOrarioFine();
            }
        }

        // dopo l'ultima lezione, se c'è spazio fino a fine giornata
        if (corrente.isBefore(fineGiornata)) {
            fasce.add(new FasciaOrariaDisponibile(corrente, fineGiornata));
        }

        return fasce;
    }

    // -------------------------
    // Catch-all (errore generico)
    // -------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Si è verificato un errore imprevisto");
    }

    // -------------------------
    // Metodo per creare JSON uniforme
    // -------------------------
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", message
                )
        );
    }
}
