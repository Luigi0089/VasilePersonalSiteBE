package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LezionePubblicaDto;
import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;

import java.time.LocalDate;
import java.util.List;

public interface LezioneService {

     LezioneResponseDto creaLezione(LezioneRequestDto lezione);

     LezioneResponseDto modificaLezione(LezioneRequestDto lezione);

     LezioneResponseDto annullaLezione(LezioneRequestDto lezione);

     PrenotazioneLezione trovaLezioneById(String id);

     List<LezionePubblicaDto> trovaLezioniSettimana(LocalDate giornoQualsiasiDellaSettimana);

     List<LezioneResponseDto> trovaLezioniSettimanaConfermate(LocalDate giornoQualsiasiDellaSettimana);

     List<LezioneResponseDto> trovaLezioniSettimanaDaConfermare(LocalDate giornoQualsiasiDellaSettimana);

     List<LezioneResponseDto> trovaLezioniSettimanaDiProva(LocalDate giornoQualsiasiDellaSettimana);

     long eliminaPrenotazioniAlmeno30GiorniPrima(LocalDate dataRiferimento);

     String confermaLezione( String id);

     String rifiutaLezione( String id);

     String posticipaLezione( String id);

     Long countLezioniConfermateSettimana(LocalDate dataRiferimento);

     Long countLezioniDaConfermareSettimana(LocalDate dataRiferimento);

     Long countLezioniDiProvaSettimana(LocalDate dataRiferimento);
}

