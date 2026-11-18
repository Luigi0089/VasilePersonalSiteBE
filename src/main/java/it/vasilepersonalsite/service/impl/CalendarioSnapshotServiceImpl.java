package it.vasilepersonalsite.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.DTO.CalendarioSnapshotDto;
import it.vasilepersonalsite.DTO.CalendarioSnapshotDto.Occupato;
import it.vasilepersonalsite.DTO.CalendarioSnapshotEsitoDto;
import it.vasilepersonalsite.DTO.CalendarioSnapshotEsitoDto.Esito;
import it.vasilepersonalsite.config.CalendarioSnapshotProperties;
import it.vasilepersonalsite.exception.GoogleDriveException;
import it.vasilepersonalsite.service.CalendarioSnapshotService;
import it.vasilepersonalsite.service.GoogleDriveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Genera lo snapshot del calendario con la STESSA regola dell'endpoint pubblico
 * GET /lezioni/settimana: ogni lezione non annullata occupa il proprio intervallo,
 * qualunque sia lo stato. Il frontend ricostruisce la griglia con la logica che usa già.
 * Copertura: dal lunedì della settimana corrente (ora italiana) per N settimane, fino al
 * sabato. Così il passaggio di giorno o di settimana senza riavvio resta coperto, e il
 * frontend tratta come "non disponibile offline" qualsiasi data fuori intervallo.
 */
@Slf4j
@Service
public class CalendarioSnapshotServiceImpl implements CalendarioSnapshotService {

    public static final int SCHEMA_VERSION = 1;
    static final String NOME_FILE = "vasile-calendario-snapshot.json";
    private static final ZoneId FUSO = ZoneId.of("Europe/Rome");

    private final LezioneDao lezioneDao;
    private final GoogleDriveService drive;
    private final CalendarioSnapshotProperties props;
    private final ObjectMapper objectMapper;

    /* iniettato via setter: i due bean si riferiscono a vicenda (pubblicazione manuale ↔ shutdown) */
    private CalendarioSnapshotShutdownPublisher shutdownPublisher;

    public CalendarioSnapshotServiceImpl(LezioneDao lezioneDao, GoogleDriveService drive,
                                         CalendarioSnapshotProperties props, ObjectMapper objectMapper) {
        this.lezioneDao = lezioneDao;
        this.drive = drive;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Autowired
    @Lazy
    void setShutdownPublisher(CalendarioSnapshotShutdownPublisher shutdownPublisher) {
        this.shutdownPublisher = shutdownPublisher;
    }

    @Override
    public CalendarioSnapshotDto genera() {
        LocalDate oggi = LocalDate.now(FUSO);
        LocalDate dal = oggi.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate al = dal.plusWeeks(props.settimane() - 1L).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        // stessa query (e quindi stesse regole) di LezioneServiceImpl.trovaLezioniSettimana
        List<Occupato> occupati = lezioneDao
                .findByDataLezioneBetweenAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(dal, al)
                .stream()
                // solo intervallo e stato: nessun dato personale, materia o id
                .map(l -> new Occupato(l.getDataLezione(), l.getOrarioInizio(), l.getOrarioFine(), l.getStato()))
                .toList();

        return new CalendarioSnapshotDto(SCHEMA_VERSION, Instant.now(), FUSO.getId(), dal, al, occupati);
    }

    /** Serializza in memoria PRIMA di contattare Drive: non si invia mai un contenuto parziale. */
    byte[] serializza(CalendarioSnapshotDto snapshot) {
        try {
            return objectMapper.writeValueAsBytes(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Serializzazione dello snapshot non riuscita", e);
        }
    }

    /** Pubblicazione con scadenza assoluta: usata sia dall'admin sia dallo shutdown. */
    CalendarioSnapshotEsitoDto pubblicaEntro(CalendarioSnapshotDto snapshot, Instant scadenza) {
        if (!props.enabled()) {
            return esito(Esito.DISABILITATO, "Snapshot del calendario disattivato (CALENDARIO_SNAPSHOT_ENABLED)", snapshot);
        }
        if (!props.pubblicazioneConfigurata()) {
            return esito(Esito.NON_CONFIGURATO,
                    "Configurazione incompleta: servono client id/secret, refresh token drive.file e id del file",
                    snapshot);
        }
        byte[] json = serializza(snapshot);
        try {
            drive.aggiornaContenuto(props.driveFileId(), json, scadenza);
            return esito(Esito.PUBBLICATO, "Snapshot pubblicato su Google Drive", snapshot);
        } catch (GoogleDriveException e) {
            // il file su Drive resta quello precedente: è il fallback per il frontend
            return esito(Esito.ERRORE_DRIVE, e.getMessage(), snapshot);
        }
    }

    @Override
    public CalendarioSnapshotEsitoDto pubblica(Duration timeout) {
        CalendarioSnapshotDto snapshot = genera();
        CalendarioSnapshotEsitoDto r = pubblicaEntro(snapshot, Instant.now().plus(timeout));
        if (r.esito() == Esito.PUBBLICATO && shutdownPublisher != null) {
            // Drive è allineato: allo shutdown non serve ripubblicare (salvo nuove modifiche)
            shutdownPublisher.segnalaPubblicato(snapshot);
        }
        log.info("SNAPSHOT CALENDARIO (admin) → {}: {}", r.esito(), r.messaggio());
        return r;
    }

    @Override
    public CalendarioSnapshotEsitoDto inizializza(Duration timeout) {
        if (!props.enabled()) {
            return esito(Esito.DISABILITATO, "Snapshot del calendario disattivato (CALENDARIO_SNAPSHOT_ENABLED)", null);
        }
        if (props.driveFileId() != null) {
            return esito(Esito.GIA_CONFIGURATO,
                    "CALENDARIO_SNAPSHOT_DRIVE_FILE_ID è già impostato: usa la pubblicazione, non serve un nuovo file",
                    null);
        }
        if (!props.credenzialiPresenti()) {
            return esito(Esito.NON_CONFIGURATO, "Servono client id/secret e refresh token con scope drive.file", null);
        }

        CalendarioSnapshotDto snapshot = genera();
        try {
            String fileId = drive.creaFilePubblico(NOME_FILE, serializza(snapshot), timeout);
            log.info("SNAPSHOT CALENDARIO → file creato su Drive, impostare CALENDARIO_SNAPSHOT_DRIVE_FILE_ID");
            return new CalendarioSnapshotEsitoDto(Esito.CREATO,
                    "File creato e reso pubblico in sola lettura. Imposta CALENDARIO_SNAPSHOT_DRIVE_FILE_ID con questo id "
                            + "nel backend e nel frontend, poi ridistribuisci.",
                    snapshot.generatedAt(), snapshot.coperturaDal(), snapshot.coperturaAl(),
                    snapshot.occupati().size(), fileId);
        } catch (GoogleDriveException e) {
            log.warn("SNAPSHOT CALENDARIO → creazione file non riuscita: {}", e.getMessage());
            return esito(Esito.ERRORE_DRIVE, e.getMessage(), snapshot);
        }
    }

    private CalendarioSnapshotEsitoDto esito(Esito esito, String messaggio, CalendarioSnapshotDto s) {
        return new CalendarioSnapshotEsitoDto(esito, messaggio,
                s == null ? null : s.generatedAt(),
                s == null ? null : s.coperturaDal(),
                s == null ? null : s.coperturaAl(),
                s == null ? null : s.occupati().size(),
                props.driveFileId());
    }
}
