package it.vasilepersonalsite.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.vasilepersonalsite.config.CalendarioSnapshotProperties;
import it.vasilepersonalsite.exception.GoogleDriveException;
import it.vasilepersonalsite.service.GoogleDriveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Client Google Drive v3 basato sul {@link HttpClient} del JDK (nessuna nuova dipendenza).
 * Autenticazione: OAuth2 con un refresh token dedicato allo scope
 * {@code https://www.googleapis.com/auth/drive.file}, distinto da quello di Google Calendar.
 * Con drive.file l'app vede SOLO i file che ha creato lei: per questo il file dello snapshot
 * va creato tramite {@link #creaFilePubblico} (endpoint admin), non a mano dall'interfaccia Drive.
 * Ogni richiesta rispetta una scadenza assoluta: durante lo shutdown il budget è di circa 1,5 s.
 */
@Slf4j
@Service
public class GoogleDriveServiceImpl implements GoogleDriveService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files/";
    private static final String CREATE_URL =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files/";

    private final CalendarioSnapshotProperties props;
    private final ObjectMapper objectMapper;
    /* creato al primo uso: HttpClient avvia un thread interno, inutile se Drive non viene usato */
    private volatile HttpClient http;

    /* access token riusato finché valido (con margine): allo shutdown spesso evita una chiamata */
    private volatile String accessToken;
    private volatile Instant scadenzaToken = Instant.EPOCH;

    public GoogleDriveServiceImpl(CalendarioSnapshotProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    private HttpClient http() {
        HttpClient c = http;
        if (c == null) {
            synchronized (this) {
                if (http == null) {
                    http = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofMillis(props.connectTimeoutMs()))
                            .version(HttpClient.Version.HTTP_1_1)
                            .build();
                }
                c = http;
            }
        }
        return c;
    }

    @Override
    public void aggiornaContenuto(String fileId, byte[] json, Instant scadenza) {
        String token = accessToken(scadenza);

        // PATCH uploadType=media: sostituisce il contenuto dello stesso file, metadati invariati
        HttpRequest req = HttpRequest.newBuilder(URI.create(UPLOAD_URL
                        + URLEncoder.encode(fileId, StandardCharsets.UTF_8) + "?uploadType=media&fields=id"))
                .timeout(timeoutEntro(scadenza))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .method("PATCH", HttpRequest.BodyPublishers.ofByteArray(json))
                .build();

        HttpResponse<String> res = invia(req, "aggiornamento file");
        if (res.statusCode() == 401) {
            // token revocato o scaduto in anticipo: alla prossima chiamata ne verrà chiesto un altro
            invalidaToken();
        }
        verificaSuccesso(res, "aggiornamento file");
    }

    @Override
    public String creaFilePubblico(String nomeFile, byte[] json, Duration timeout) {
        Instant scadenza = Instant.now().plus(timeout);
        String token = accessToken(scadenza);

        String boundary = "snapshot-" + UUID.randomUUID();
        String metadati = scriviJson(Map.of("name", nomeFile, "mimeType", "application/json"));
        byte[] corpo = concatena(
                ("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                        + metadati + "\r\n--" + boundary
                        + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8),
                json,
                ("\r\n--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> creato = invia(HttpRequest.newBuilder(URI.create(CREATE_URL))
                .timeout(timeoutEntro(scadenza))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                .build(), "creazione file");
        verificaSuccesso(creato, "creazione file");

        String fileId = leggiCampo(creato.body(), "id");
        if (fileId == null) {
            throw new GoogleDriveException("Drive non ha restituito l'id del file creato");
        }

        // lettura pubblica in sola lettura: necessaria perché il frontend lo scarichi senza login
        HttpResponse<String> permesso = invia(HttpRequest.newBuilder(URI.create(FILES_URL
                        + URLEncoder.encode(fileId, StandardCharsets.UTF_8) + "/permissions?fields=id"))
                .timeout(timeoutEntro(scadenza))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        scriviJson(Map.of("type", "anyone", "role", "reader"))))
                .build(), "condivisione pubblica");
        verificaSuccesso(permesso, "condivisione pubblica (file " + fileId + " creato ma non pubblico)");

        return fileId;
    }

    // ------------------------------------------------------------------ OAuth

    private String accessToken(Instant scadenza) {
        String cached = accessToken;
        if (cached != null && Instant.now().isBefore(scadenzaToken)) {
            return cached;
        }
        synchronized (this) {
            if (accessToken != null && Instant.now().isBefore(scadenzaToken)) {
                return accessToken;
            }
            if (!props.credenzialiPresenti()) {
                throw new GoogleDriveException("Credenziali OAuth per Google Drive non configurate");
            }
            String form = "client_id=" + enc(props.clientId())
                    + "&client_secret=" + enc(props.clientSecret())
                    + "&refresh_token=" + enc(props.refreshToken())
                    + "&grant_type=refresh_token";

            HttpResponse<String> res = invia(HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .timeout(timeoutEntro(scadenza))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build(), "rinnovo access token");
            // il body di errore OAuth non contiene segreti, ma non lo si logga comunque
            if (res.statusCode() / 100 != 2) {
                throw new GoogleDriveException("Rinnovo access token rifiutato da Google", res.statusCode(), null);
            }

            String token = leggiCampo(res.body(), "access_token");
            if (token == null) {
                throw new GoogleDriveException("Google non ha restituito un access token");
            }
            String expires = leggiCampo(res.body(), "expires_in");
            long secondi = 3600;
            try {
                if (expires != null) secondi = Long.parseLong(expires);
            } catch (NumberFormatException ignored) {
                // si usa il default
            }
            accessToken = token;
            scadenzaToken = Instant.now().plusSeconds(Math.max(secondi - 60, 0));
            return token;
        }
    }

    private void invalidaToken() {
        accessToken = null;
        scadenzaToken = Instant.EPOCH;
    }

    // ------------------------------------------------------------------ HTTP

    private HttpResponse<String> invia(HttpRequest req, String operazione) {
        try {
            return http().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new GoogleDriveException("Timeout Google Drive (" + operazione + ")", e);
        } catch (IOException e) {
            throw new GoogleDriveException("Errore di rete verso Google Drive (" + operazione + "): "
                    + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleDriveException("Richiesta a Google Drive interrotta (" + operazione + ")", e);
        }
    }

    private static void verificaSuccesso(HttpResponse<String> res, String operazione) {
        int s = res.statusCode();
        if (s / 100 == 2) {
            return;
        }
        String dettaglio = switch (s) {
            case 401 -> "token non valido o revocato";
            case 403 -> "permesso negato (scope drive.file e file non creato da questa app?) o quota superata";
            case 404 -> "file non trovato o non accessibile con drive.file";
            case 429 -> "troppe richieste";
            default -> s >= 500 ? "errore temporaneo di Google" : "richiesta rifiutata";
        };
        throw new GoogleDriveException("Google Drive " + operazione + ": HTTP " + s + " - " + dettaglio, s, null);
    }

    private Duration timeoutEntro(Instant scadenza) {
        long rimanenti = Duration.between(Instant.now(), scadenza).toMillis();
        if (rimanenti < 100) {
            throw new GoogleDriveException("Tempo disponibile esaurito prima della richiesta a Google Drive");
        }
        return Duration.ofMillis(Math.min(rimanenti, props.requestTimeoutMs()));
    }

    // ------------------------------------------------------------------ utilità

    private String leggiCampo(String json, String campo) {
        try {
            JsonNode n = objectMapper.readTree(json).get(campo);
            return n == null || n.isNull() ? null : n.asText();
        } catch (IOException e) {
            return null;
        }
    }

    private String scriviJson(Map<String, ?> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (IOException e) {
            throw new GoogleDriveException("Serializzazione metadati non riuscita", e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static byte[] concatena(byte[]... parti) {
        int len = 0;
        for (byte[] p : parti) len += p.length;
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] p : parti) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
