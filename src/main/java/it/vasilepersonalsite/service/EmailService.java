package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;

public interface EmailService {

    public void sendNotificaLezione(LezioneResponseDto lezione, String mailStudente, boolean isModifca);

    public void confermaLezione(PrenotazioneLezione lezione);

    public void annullaLezione(PrenotazioneLezione lezione);

    public void posticipaLezione(PrenotazioneLezione lezione);

}
