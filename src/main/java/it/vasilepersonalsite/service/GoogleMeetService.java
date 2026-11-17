package it.vasilepersonalsite.service;

import it.vasilepersonalsite.entity.PrenotazioneLezione;

public interface GoogleMeetService {

    /**
     * Crea su Google Calendar un evento con videochiamata Google Meet per la lezione
     * e ne restituisce il link.
     *
     * Non lancia mai eccezioni: se Google non è configurato o la chiamata fallisce
     * restituisce {@code null}, così la conferma della lezione non viene bloccata.
     *
     * @return link Meet (es. https://meet.google.com/abc-defg-hij) oppure null
     */
    String creaLinkMeet(PrenotazioneLezione lezione);
}
