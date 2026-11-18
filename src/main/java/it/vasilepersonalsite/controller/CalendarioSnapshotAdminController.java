package it.vasilepersonalsite.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.vasilepersonalsite.DTO.CalendarioSnapshotDto;
import it.vasilepersonalsite.DTO.CalendarioSnapshotEsitoDto;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.CalendarioSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Operazioni amministrative sullo snapshot del calendario.
 * Sotto /admin/** → protette dalla regola esistente hasRole("ADMIN") di SecurityConfig
 * (nessuna modifica alle regole di sicurezza).
 */
@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.ADMIN_PATH + "/calendario-snapshot")
public class CalendarioSnapshotAdminController {

    /* fuori dallo shutdown non c'è il limite dei 2 s: margine più ampio per Drive */
    private static final Duration TIMEOUT_ADMIN = Duration.ofSeconds(10);

    private final CalendarioSnapshotService snapshotService;

    public CalendarioSnapshotAdminController(CalendarioSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @Operation(summary = "Anteprima dello snapshot", description = "Mostra il JSON che verrebbe pubblicato, senza contattare Google Drive")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/anteprima")
    public ResponseEntity<CalendarioSnapshotDto> anteprima() {
        return ResponseEntity.ok(snapshotService.genera());
    }

    @Operation(summary = "Rigenera e pubblica lo snapshot", description = "Sovrascrive il file configurato su Google Drive")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/pubblica")
    public ResponseEntity<CalendarioSnapshotEsitoDto> pubblica() {
        return risposta(snapshotService.pubblica(TIMEOUT_ADMIN));
    }

    @Operation(summary = "Prima inizializzazione",
            description = "Crea il file pubblico su Google Drive (scope drive.file) e restituisce l'id da configurare")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/inizializza")
    public ResponseEntity<CalendarioSnapshotEsitoDto> inizializza() {
        return risposta(snapshotService.inizializza(TIMEOUT_ADMIN));
    }

    private static ResponseEntity<CalendarioSnapshotEsitoDto> risposta(CalendarioSnapshotEsitoDto e) {
        HttpStatus status = switch (e.esito()) {
            case PUBBLICATO -> HttpStatus.OK;
            case CREATO -> HttpStatus.CREATED;
            case GIA_CONFIGURATO -> HttpStatus.CONFLICT;
            case DISABILITATO, NON_CONFIGURATO -> HttpStatus.SERVICE_UNAVAILABLE;
            case ERRORE_DRIVE -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).body(e);
    }
}
