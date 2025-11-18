package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.exception.ConflictException;
import it.vasilepersonalsite.exception.LessonNotFoundException;
import it.vasilepersonalsite.exception.NoMatchCodeException;
import it.vasilepersonalsite.service.LezioneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LezioneServiceImpl implements LezioneService {

    @Autowired
    LezioneDao lezioneDao;

    private static final LocalTime WEEKDAY_START = LocalTime.of(18, 0);
    private static final LocalTime WEEKDAY_END   = LocalTime.of(21, 0);

    /**
     * @param lezione
     * @return
     */
    @Override
    public LezioneResponseDto creaLezione(LezioneRequestDto lezione) {
        if (lezione == null) {
            log.error("PRENOTA LEZIONE → richiesta NULL, impossibile prenotare");
            throw new IllegalArgumentException("Lezione nulla");
        }

        log.info("PRENOTA LEZIONE → richiesta di prenotazione: {}", lezione);

        validaRegole(lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());
        verificaDisponibilitaOrario(
                lezione.getDataLezione(),
                lezione.getOrarioInizio(),
                lezione.getOrarioFine(),
                null   // nessuna lezione da escludere
        );

        PrenotazioneLezione lezionePrenotata = new PrenotazioneLezione(lezione);
        lezioneDao.save(lezionePrenotata);

        log.info("PRENOTA LEZIONE → lezione prenotata, ID={} codice prenotazione={}",
                lezionePrenotata.getId(), lezionePrenotata.getCodiceModifica());

        return new LezioneResponseDto(lezionePrenotata);
    }


    private void verificaDisponibilitaOrario(
            LocalDate dataLezione,
            LocalTime oraInizio,
            LocalTime oraFine,
            String idDaEscludere   // null per creazione, != null per modifica
    ) {
        log.info("VERIFICA ORARIO → data={} inizio={} fine={} (escludi ID={})",
                dataLezione, oraInizio, oraFine, idDaEscludere);

        List<PrenotazioneLezione> sovrapposte =
                lezioneDao.findByDataLezioneAndOrarioInizioLessThanAndOrarioFineGreaterThan(
                        dataLezione,
                        oraFine,
                        oraInizio
                );

        // filtro eventuale annullata + la lezione stessa in caso di modifica
        boolean esisteConflitto = sovrapposte.stream()
                .filter(l -> !l.isAnnullata())
                .anyMatch(l -> idDaEscludere == null || !l.getId().equals(idDaEscludere));

        if (esisteConflitto) {
            log.error("VERIFICA ORARIO → conflitto trovato per data={} inizio={} fine={}",
                    dataLezione, oraInizio, oraFine);
            throw new ConflictException(dataLezione, oraInizio, oraFine);

        }

        log.info("VERIFICA ORARIO → nessun conflitto trovato");
    }


    private void validaRegole(LocalDate dataLezione, LocalTime oraInizio, LocalTime oraFine) {

        if (!oraInizio.isBefore(oraFine)) {
            throw new ConflictException("L'orario di inizio deve essere antecedente all'orario di fine");
        }

        // 2) Regole per giorno della settimana
        DayOfWeek dow = dataLezione.getDayOfWeek();

        if (dow == DayOfWeek.SUNDAY) {
            throw new ConflictException("La domenica non è possibile prenotare lezioni");
        }

        if (dow.getValue() >= DayOfWeek.MONDAY.getValue() && dow.getValue() <= DayOfWeek.FRIDAY.getValue()) {
            // Lun–Ven: consentito solo 18:00–21:00 (estremi inclusi)
            boolean inizioOk = !oraInizio.isBefore(WEEKDAY_START);
            boolean fineOk   = !oraFine.isAfter(WEEKDAY_END);
            if (!(inizioOk && fineOk)) {
                throw new ConflictException(
                        String.format("Dal lunedì al venerdì è possibile prenotare solo tra %s e %s.",
                                WEEKDAY_START, WEEKDAY_END)
                );
            }
        }
        // Sabato: tutto il giorno → nessun vincolo extra
    }


    /**
     * @param lezione
     * @return
     */
    @Override
    public LezioneResponseDto modificaLezione(LezioneRequestDto lezione) {
        log.info("MODIFICA LEZIONE = {}", lezione);
        PrenotazioneLezione prenotazioneLezione = trovaLezioneById(lezione.getId());
        if (prenotazioneLezione == null) {
            log.error("LEZIONE {} NON TROVATA! ", lezione.getId());
            throw new LessonNotFoundException(lezione.getNomeStudente(), lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());
        }
        if(lezione.getCodiceModifica()!=null &&
                lezione.getCodiceModifica().equals(Objects.requireNonNull(prenotazioneLezione).getCodiceModifica())){

            validaRegole(lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());

            String nomeStudente = lezione.getNomeStudente();
            nomeStudente = nomeStudente.replaceAll(" ", "_");
            verificaDisponibilitaOrario(
                    lezione.getDataLezione(),
                    lezione.getOrarioInizio(),
                    lezione.getOrarioFine(),
                    prenotazioneLezione.getId()
            );
            prenotazioneLezione.setDataLezione(lezione.getDataLezione());
            prenotazioneLezione.setOrarioInizio(lezione.getOrarioInizio());
            prenotazioneLezione.setOrarioFine(lezione.getOrarioFine());
            prenotazioneLezione.setLivello(lezione.getLivello());
            prenotazioneLezione.setNote(lezione.getNote());

            lezioneDao.save(prenotazioneLezione);

        }else {
            log.error("CODICE MODIFICA ERRATO!");
            throw new NoMatchCodeException(lezione.getId(), lezione.getCodiceModifica());
        }
        return new LezioneResponseDto(prenotazioneLezione);
    }

    /**
     * @param lezione
     * @return
     */
    @Override
    public LezioneResponseDto annullaLezione(LezioneRequestDto lezione) {

        PrenotazioneLezione lezionePrenotata = trovaLezioneById(lezione.getId());

        if (lezionePrenotata == null) {
            log.error("LEZIONE {} NON TROVATA! ", lezione.getId());
            throw new LessonNotFoundException(lezione.getNomeStudente(), lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());
        }
        else if(lezione.getCodiceModifica()!=null &&
                lezione.getCodiceModifica().equals(Objects.requireNonNull(lezionePrenotata).getCodiceModifica())) {

            lezionePrenotata.setAnnullata(true);
            lezioneDao.save(lezionePrenotata);

            log.info("TROVA LEZIONE → Lezione ID={} annullata correttamente",
                    lezione.getId());

            return new LezioneResponseDto(lezionePrenotata);
        } else {
            log.error("CODICE MODIFICA ERRATO!");
            throw new NoMatchCodeException(lezione.getId(), lezione.getCodiceModifica());
        }
    }


    /**
     * @param id
     * @return
     */
    @Override
    public PrenotazioneLezione trovaLezioneById(String id) {
        log.info("TROVA LEZIONE → Ricerca per ID: {}", id);

        Optional<PrenotazioneLezione> lezione = lezioneDao.findById(id);

        log.info("TROVA LEZIONE → Risultato per ID {}: {}",
                id, lezione.isPresent() ? "Trovata" : "Non trovata");

        return lezione.orElse(null);
    }

    /**
     * @param giornoQualsiasiDellaSettimana
     * @return
     */
    @Override
    public List<LezioneResponseDto> trovaLezioniSettimana(LocalDate giornoQualsiasiDellaSettimana) {
        LocalDate lunedi = giornoQualsiasiDellaSettimana.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sabato = giornoQualsiasiDellaSettimana.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));


        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneBetweenAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(lunedi, sabato);

        List<LezioneResponseDto> lezioneResponseDto = lezioni.stream().map(lezione -> new LezioneResponseDto(lezione)).collect(Collectors.toList());
        return lezioneResponseDto;
    }


}
