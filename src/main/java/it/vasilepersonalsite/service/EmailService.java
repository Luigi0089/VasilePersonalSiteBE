package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LezioneResponseDto;
import it.vasilepersonalsite.entity.PrenotazioneLezione;

public interface EmailService {

    public void sendLezionePrenotataEmail(LezioneResponseDto lezione, String mailStudente);

    public void confermaLezione(PrenotazioneLezione lezione);

    public void annullaLezione(PrenotazioneLezione lezione);

    public void posticipaLezione(PrenotazioneLezione lezione);
}
