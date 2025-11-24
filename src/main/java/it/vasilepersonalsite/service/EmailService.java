package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;

public interface EmailService {

     void sendNotificaLezione(LezioneResponseDto lezione, String mailStudente, boolean isModifca);

     void confermaLezione(PrenotazioneLezione lezione);

     void annullaLezione(PrenotazioneLezione lezione);


}
