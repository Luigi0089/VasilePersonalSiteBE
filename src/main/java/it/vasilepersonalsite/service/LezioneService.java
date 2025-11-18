package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LezioneRequestDto;
import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

public interface LezioneService {

    public LezioneResponseDto creaLezione(LezioneRequestDto lezione);

    public LezioneResponseDto modificaLezione(LezioneRequestDto lezione);

    public LezioneResponseDto annullaLezione(LezioneRequestDto lezione);

    public PrenotazioneLezione trovaLezioneById(String id);

    public List<LezioneResponseDto> trovaLezioniSettimana(LocalDate giornoQualsiasiDellaSettimana);


}

