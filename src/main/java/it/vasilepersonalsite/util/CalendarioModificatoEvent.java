package it.vasilepersonalsite.util;

/**
 * Evento applicativo: le disponibilità pubbliche del calendario sono cambiate
 * (prenotazione, modifica, annullamento, conferma, rifiuto, pulizia mensile).
 * Non scrive nulla su Google Drive: serve solo a ricordare che allo shutdown
 * lo snapshot va ripubblicato. Pubblicato DOPO il commit della transazione.
 */
public record CalendarioModificatoEvent(String motivo) {
}
