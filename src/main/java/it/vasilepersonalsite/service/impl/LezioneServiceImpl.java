package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.DTO.LezionePubblicaDto;
import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.constans.Stato;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.exception.ConflictException;
import it.vasilepersonalsite.exception.LessonNotFoundException;
import it.vasilepersonalsite.exception.NoMatchCodeException;
import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.service.GoogleMeetService;
import it.vasilepersonalsite.service.LezioneService;
import it.vasilepersonalsite.util.SimpleAES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LezioneServiceImpl implements LezioneService {

    @Autowired
    LezioneDao lezioneDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleMeetService googleMeetService;

    @Value("${chiave.SimpleAES}")
    private String chiave;

    @Value("${admin.password}")
    private String universalPassword;

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

        if (lezione.isLezioneDiProva()) {
            validaDurataLezioneDiProva(lezione.getOrarioInizio(), lezione.getOrarioFine());
        }

        verificaDisponibilitaOrario(
                lezione.getDataLezione(),
                lezione.getOrarioInizio(),
                lezione.getOrarioFine(),
                null   // nessuna lezione da escludere
        );

        PrenotazioneLezione lezionePrenotata = new PrenotazioneLezione(lezione);
        String codiceModifica = lezionePrenotata.getCodiceModifica();
        lezionePrenotata.setCodiceModifica(SimpleAES.cripta(codiceModifica, chiave));
        lezioneDao.save(lezionePrenotata);
        lezionePrenotata.setCodiceModifica(codiceModifica);


        log.info("PRENOTA LEZIONE → lezione prenotata, ID={} codice prenotazione={}",
                lezionePrenotata.getId(), lezionePrenotata.getCodiceModifica());

        // mappa DTO risposta
        LezioneResponseDto response = new LezioneResponseDto(lezionePrenotata);

        // invio email (senza far fallire la prenotazione)
        try {
            emailService.sendNotificaLezione(response, lezione.getEmail(), false);
        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL: " + e.getMessage());
        }


        return response;
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
                lezioneDao.findByDataLezioneAndOrarioInizioLessThanAndOrarioFineGreaterThanAndAnnullataFalse(
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
        PrenotazioneLezione lezionePrenotata = trovaLezioneById(lezione.getId());
        if (lezionePrenotata == null) {
            log.error("LEZIONE {} NON TROVATA! ", lezione.getId());
            throw new LessonNotFoundException(lezione.getNomeStudente(), lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());
        }
        if (codiceModificaValido(lezione.getCodiceModifica(), lezionePrenotata)) {

            validaRegole(lezione.getDataLezione(), lezione.getOrarioInizio(), lezione.getOrarioFine());

            // una lezione di prova resta di un'ora anche se modificata
            if (lezionePrenotata.isLezioneDiProva()) {
                validaDurataLezioneDiProva(lezione.getOrarioInizio(), lezione.getOrarioFine());
            }

            verificaDisponibilitaOrario(
                    lezione.getDataLezione(),
                    lezione.getOrarioInizio(),
                    lezione.getOrarioFine(),
                    lezionePrenotata.getId()
            );
            lezionePrenotata.setDataLezione(lezione.getDataLezione());
            lezionePrenotata.setOrarioInizio(lezione.getOrarioInizio());
            lezionePrenotata.setOrarioFine(lezione.getOrarioFine());
            lezionePrenotata.setLivello(lezione.getLivello());

            // il calendario pubblico non espone le note salvate: le sovrascrivo
            // solo se lo studente ne ha scritte di nuove, altrimenti restano quelle originali
            if (lezione.getNote() != null && !lezione.getNote().isBlank()) {
                lezionePrenotata.setNote(lezione.getNote());
            }

            lezioneDao.save(lezionePrenotata);

            lezionePrenotata.setCodiceModifica(SimpleAES.decripta(Objects.requireNonNull(lezionePrenotata).getCodiceModifica(), chiave));


        }else {
            log.error("CODICE MODIFICA ERRATO!");
            throw new NoMatchCodeException(lezione.getId(), lezione.getCodiceModifica());
        }

        // mappa DTO risposta
        LezioneResponseDto response = new LezioneResponseDto(lezionePrenotata);

        // invio email (senza far fallire la prenotazione).
        // Uso l'email salvata: quella della richiesta non è più affidabile/presente.
        try {
            emailService.sendNotificaLezione(response, lezionePrenotata.getEmail(), true);
        } catch (Exception e) {
            log.error("ERRORE INVIO EMAIL: " + e.getMessage());
        }

        return response;
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
        else if (codiceModificaValido(lezione.getCodiceModifica(), lezionePrenotata)) {

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
    public List<LezionePubblicaDto> trovaLezioniSettimana(LocalDate giornoQualsiasiDellaSettimana) {
        LocalDate lunedi = giornoQualsiasiDellaSettimana.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sabato = giornoQualsiasiDellaSettimana.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneBetweenAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(lunedi, sabato);

        // endpoint pubblico: solo materia, livello, data, orari e stato (niente dati personali)
        return lezioni.stream()
                .map(LezionePubblicaDto::new)
                .collect(Collectors.toList());
    }

    /*
     * Codice valido se coincide con quello della lezione (case-insensitive,
     * i codici sono generati in maiuscolo) oppure con la password universale.
     * Null-safe: prima un codice mancante causava NullPointerException.
     */
    private boolean codiceModificaValido(String codice, PrenotazioneLezione lezione) {
        if (codice == null || codice.isBlank()) {
            return false;
        }

        String codiceLezione = SimpleAES.decripta(lezione.getCodiceModifica(), chiave);

        return codice.trim().equalsIgnoreCase(codiceLezione)
                || codice.equals(universalPassword);
    }

    /**
     * @param giornoQualsiasiDellaSettimana
     * @return
     */
    @Override
    public List<LezioneResponseDto> trovaLezioniSettimanaConfermate(
            LocalDate giornoQualsiasiDellaSettimana) {

        log.info(
                "Ricerca lezioni confermate a partire dal giorno: {}",
                giornoQualsiasiDellaSettimana
        );

        LocalDate sabato = giornoQualsiasiDellaSettimana.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        );

        log.debug(
                "Intervallo ricerca lezioni confermate: dal {} al {}",
                giornoQualsiasiDellaSettimana,
                sabato
        );

        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneBetweenAndStatoAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(
                        giornoQualsiasiDellaSettimana,
                        sabato,
                        Stato.CONFERMATA.getLabel()
                );

        log.info(
                "Trovate {} lezioni confermate nell'intervallo dal {} al {}",
                lezioni.size(),
                giornoQualsiasiDellaSettimana,
                sabato
        );

        return lezioni.stream()
                .map(LezioneResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Trova le lezioni in attesa di conferma nell'intervallo indicato.
     *
     * @param giornoQualsiasiDellaSettimana giorno iniziale della ricerca
     * @return lezioni in attesa di conferma
     */
    @Override
    public List<LezioneResponseDto> trovaLezioniSettimanaDaConfermare(
            LocalDate giornoQualsiasiDellaSettimana) {

        log.info(
                "Ricerca lezioni da confermare a partire dal giorno: {}",
                giornoQualsiasiDellaSettimana
        );

        LocalDate sabato = giornoQualsiasiDellaSettimana.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        );

        log.debug(
                "Intervallo ricerca lezioni da confermare: dal {} al {}",
                giornoQualsiasiDellaSettimana,
                sabato
        );

        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneBetweenAndStatoAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(
                        giornoQualsiasiDellaSettimana,
                        sabato,
                        Stato.IN_APPROVAZIONE.getLabel()
                );

        log.info(
                "Trovate {} lezioni da confermare nell'intervallo dal {} al {}",
                lezioni.size(),
                giornoQualsiasiDellaSettimana,
                sabato
        );

        return lezioni.stream()
                .map(LezioneResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Trova le lezioni di prova (non annullate) dal giorno indicato fino al sabato
     * della stessa settimana, come per le lezioni confermate / da confermare.
     *
     * @param giornoQualsiasiDellaSettimana giorno iniziale della ricerca
     * @return lezioni di prova dell'intervallo
     */
    @Override
    public List<LezioneResponseDto> trovaLezioniSettimanaDiProva(
            LocalDate giornoQualsiasiDellaSettimana) {

        LocalDate sabato = giornoQualsiasiDellaSettimana.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
        );

        log.info("Ricerca lezioni di prova dal {} al {}",
                giornoQualsiasiDellaSettimana, sabato);

        List<PrenotazioneLezione> lezioni = lezioneDao
                .findByDataLezioneBetweenAndLezioneDiProvaTrueAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(
                        giornoQualsiasiDellaSettimana,
                        sabato
                );

        log.info("Trovate {} lezioni di prova nell'intervallo dal {} al {}",
                lezioni.size(), giornoQualsiasiDellaSettimana, sabato);

        return lezioni.stream()
                .map(LezioneResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Elimina tutte le prenotazioni con dataLezione almeno 30 giorni prima di dataRiferimento.
     * Quindi: dataLezione <= dataRiferimento.minusDays(30)
     */
    @Transactional
    public long eliminaPrenotazioniAlmeno30GiorniPrima(LocalDate dataRiferimento) {
        LocalDate limite = dataRiferimento.minusDays(30);
        return lezioneDao.deleteByDataLezioneLessThanEqual(limite);
    }

    /**
     * @param id
     * @return
     */
    @Override
    public String confermaLezione(String id) {

       PrenotazioneLezione lezione = trovaLezioneById(id);

       lezione.setStato(Stato.CONFERMATA.getLabel());

       // il link Meet si crea una volta sola: se la lezione è già stata confermata si riusa
       if (lezione.getLinkMeet() == null || lezione.getLinkMeet().isBlank()) {
           lezione.setLinkMeet(googleMeetService.creaLinkMeet(lezione));
       }

       lezioneDao.save(lezione);

       emailService.confermaLezione(lezione);

        return "Lezione di giorno "+ lezione.getDataLezione() + " alle ore " + lezione.getOrarioInizio() + " Confermata";
    }

    /**
     * @param id
     * @return
     */
    @Override
    public String rifiutaLezione(String id) {

        PrenotazioneLezione lezione = trovaLezioneById(id);

        lezione.setStato(Stato.ANNULLATA.getLabel());

        lezione.setAnnullata(true);

        lezioneDao.save(lezione);

        emailService.annullaLezione(lezione);

        return "Lezione di giorno "+ lezione.getDataLezione() + " alle ore " + lezione.getOrarioInizio() + " Annullata";

    }

    /**
     * @param id
     * @return
     */
    @Override
    public String posticipaLezione(String id) {
        return "";
    }

    /**
     * @param dataRiferimento
     * @return
     */
    @Override
    public Long countLezioniConfermateSettimana(LocalDate dataRiferimento) {

        LocalDate sabato = dataRiferimento.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));


        return lezioneDao
                .countByDataLezioneBetweenAndStatoAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(dataRiferimento, sabato, Stato.CONFERMATA.getLabel());
    }

    /**
     * @param dataRiferimento
     * @return
     */
    @Override
    public Long countLezioniDaConfermareSettimana(LocalDate dataRiferimento) {
        LocalDate sabato = dataRiferimento.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));


        return lezioneDao
                .countByDataLezioneBetweenAndStatoAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(dataRiferimento, sabato, Stato.IN_APPROVAZIONE.getLabel());
    }

    /**
     * Conta le lezioni di prova (non annullate) dal giorno indicato al sabato.
     */
    @Override
    public Long countLezioniDiProvaSettimana(LocalDate dataRiferimento) {
        LocalDate sabato = dataRiferimento.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        return lezioneDao
                .countByDataLezioneBetweenAndLezioneDiProvaTrueAndAnnullataFalse(dataRiferimento, sabato);
    }

    /**
     * Le lezioni di prova durano esattamente un'ora.
     * Il controllo è lato server perché l'API di prenotazione è pubblica.
     */
    private void validaDurataLezioneDiProva(LocalTime oraInizio, LocalTime oraFine) {
        if (!Duration.between(oraInizio, oraFine).equals(Duration.ofHours(1))) {
            throw new ConflictException("La lezione di prova deve durare esattamente un'ora");
        }
    }


}
