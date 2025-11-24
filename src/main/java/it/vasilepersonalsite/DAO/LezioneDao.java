package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.PrenotazioneLezione;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

@Repository
public interface LezioneDao extends JpaRepository<PrenotazioneLezione,String> {


    PrenotazioneLezione findByNomeStudenteAndDataLezione(String nomeStudente, LocalDate dataLezione);

    PrenotazioneLezione findByDataLezioneAndOrarioInizioAndOrarioFine(LocalDate  dataLezione, LocalTime inizio, LocalTime fine);

    List<PrenotazioneLezione> findByDataLezioneAndOrarioInizioLessThanAndOrarioFineGreaterThan(
            LocalDate dataLezione,
            LocalTime oraFine,    // fine del nuovo slot
            LocalTime oraInizio   // inizio del nuovo slot
    );

    List<PrenotazioneLezione> findByDataLezioneAndAnnullataFalseOrderByOrarioInizioAsc(
            LocalDate  dataLezione
    );

    List<PrenotazioneLezione>
    findByDataLezioneBetweenAndAnnullataFalseOrderByDataLezioneAscOrarioInizioAsc(
            LocalDate dal, LocalDate al
    );

    /**
     * Elimina tutte le prenotazioni con dataLezione <= limiteInclusivo.
     */
    @Modifying
    long deleteByDataLezioneLessThanEqual(LocalDate limiteInclusivo);
}
