package it.vasilepersonalsite.exception;

import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.exception.ErrorRespons.FasciaOrariaDisponibile;
import it.vasilepersonalsite.exception.ErrorRespons.FieldValidationError;
import it.vasilepersonalsite.exception.ErrorRespons.ProfanityErrorResponse;
import it.vasilepersonalsite.exception.ErrorRespons.ValidationErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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


    /**
     * Gestisce i DTO (@RequestBody) invalidi.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        // 1) Caso speciale: parolacce
        Optional<FieldError> profanityError = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .filter(err -> "NoProfanity".equals(err.getCode()))
                .findFirst();

        if (profanityError.isPresent()) {
            FieldError fe = profanityError.get();

            ProfanityErrorResponse body = new ProfanityErrorResponse(
                    "PROFANITY_DETECTED",
                    fe.getDefaultMessage(),
                    fe.getField(),
                    fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        // 2) Tutti gli altri errori (@NotNull, @NotBlank, @Email, ecc.)
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new FieldValidationError(
                        fe.getField(),
                        fe.getDefaultMessage(),   // es: "must not be null", "must not be blank"
                        fe.getCode(),             // es: "NotNull", "NotBlank"
                        fe.getRejectedValue()
                ))
                .toList();

        ValidationErrorResponse body = new ValidationErrorResponse(
                "VALIDATION_ERROR",
                "Richiesta non valida. Verifica i campi indicati.",
                fieldErrors
        );

        return ResponseEntity.badRequest().body(body);
    }


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
