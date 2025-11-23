package it.vasilepersonalsite.util;

import it.vasilepersonalsite.service.LezioneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BatchDB {

    private static final Logger log = LoggerFactory.getLogger(BatchDB.class);


    @Autowired
    LezioneService lezioneService;
    /**
     * Batch mensile che parte il PRIMO giorno di ogni mese alle 02:00.
     * Formato cron di Spring: secondi minuti ore giornoMese mese giornoSettimana
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void eliminaLezione() {
        log.info("Avvio batch mensile - primo del mese ore 02:00");

        LocalDate oggi = LocalDate.now();
        long eliminati = lezioneService.eliminaPrenotazioniAlmeno30GiorniPrima(oggi);
        log.info("Batch mensile: eliminate {} prenotazioni con dataLezione <= {}", eliminati, oggi.minusDays(30));

        log.info("Fine batch mensile");
    }
}
