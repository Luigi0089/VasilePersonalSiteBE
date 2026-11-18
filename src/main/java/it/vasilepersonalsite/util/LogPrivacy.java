package it.vasilepersonalsite.util;

/**
 * Mascheramento dei dati personali nei log applicativi.
 * I log di App Engine sono conservati e consultabili: non devono contenere
 * indirizzi email completi degli studenti né codici di modifica in chiaro.
 */
public final class LogPrivacy {

    private LogPrivacy() {
    }

    /** "mario.rossi@gmail.com" → "m***@gmail.com"; null/non valido → "***". */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
