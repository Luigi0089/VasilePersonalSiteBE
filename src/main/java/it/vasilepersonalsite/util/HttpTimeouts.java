package it.vasilepersonalsite.util;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Timeout per le chiamate HTTP verso servizi esterni (Mailjet, Google).
 * {@link SimpleClientHttpRequestFactory} è lo stesso client usato di default da
 * {@code new RestTemplate()} (HttpURLConnection del JDK): cambia solo il fatto
 * che una connessione bloccata non può più attendere all'infinito.
 */
public final class HttpTimeouts {

    /** Apertura della connessione TCP/TLS. */
    public static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Attesa della risposta (Mailjet con allegati e Google Calendar restano ben sotto). */
    public static final int READ_TIMEOUT_MS = 15_000;

    private HttpTimeouts() {
    }

    public static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return factory;
    }
}
