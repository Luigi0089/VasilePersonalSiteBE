package it.vasilepersonalsite;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.service.GoogleMeetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Matrice di autorizzazione completa su Tomcat reale, sugli endpoint effettivamente registrati:
 *  - senza token                       -> 401
 *  - token valido, ruolo non ADMIN     -> 403
 *  - token scaduto / firma errata / manomesso / alg=none / malformato -> 401
 *  - token ADMIN valido                -> mai 401/403 (la risposta dipende dal controller)
 * e classificazione delle varianti di URL (rifiutate dal firewall, dalla catena di sicurezza
 * o senza handler). Nessuna variante deve mai produrre una risposta 2xx/3xx.
 * Il risultato dettagliato viene scritto in target/security-matrix.txt.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
class SicurezzaRuoliTest {

    private static final String API = "/luigi/vasile/personal/api";
    private static final Set<String> PUBBLICI = Set.of(
            "GET " + API + "/auth/ping", "POST " + API + "/auth/login", "POST " + API + "/auth/sblocca",
            "GET " + API + "/stack/progetti", "GET " + API + "/stack/readme", "GET " + API + "/stack/skills",
            "GET " + API + "/stack/skills/{id}", "GET " + API + "/stack/categories", "GET " + API + "/stack/keywords",
            "POST " + API + "/lezioni", "PUT " + API + "/lezioni/modifica", "PUT " + API + "/lezioni/annulla",
            "GET " + API + "/lezioni/settimana");

    @LocalServerPort int port;
    @Value("${security.jwt.secret}") String secret;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
    @MockBean EmailService emailService;
    @MockBean GoogleMeetService googleMeetService;

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private record Endpoint(String metodo, String pattern) {
        String path() { return pattern.replace("{id}", "1") + "?data=2026-11-18&id=x&password=x"; }
    }

    private List<Endpoint> riservati() {
        List<Endpoint> out = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, h) -> {
            if (!h.getBeanType().getPackageName().startsWith("it.vasilepersonalsite")) return;
            for (String p : info.getPatternValues())
                for (RequestMethod m : info.getMethodsCondition().getMethods())
                    if (!PUBBLICI.contains(m.name() + " " + p)) out.add(new Endpoint(m.name(), p));
        });
        return out;
    }

    private SecretKey chiave() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    private String jwt(String ruolo, Instant scadenza, SecretKey key) {
        var b = Jwts.builder().subject("test").issuedAt(new Date()).expiration(Date.from(scadenza));
        if (ruolo != null) b.claim("role", ruolo);
        return b.signWith(key, Jwts.SIG.HS256).compact();
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private int status(String metodo, String rawPath, String bearer) throws Exception {
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPath))
                .header("Content-Type", "application/json");
        if (bearer != null) rb.header("Authorization", "Bearer " + bearer);
        boolean body = metodo.equals("POST") || metodo.equals("PUT");
        rb.method(metodo, body ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody());
        return client.send(rb.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void matriceCompletaSuTuttiGliEndpointRiservati() throws Exception {
        List<Endpoint> eps = riservati();
        assertTrue(eps.size() >= 15, "endpoint riservati trovati: " + eps.size());

        String admin = jwt("ADMIN", Instant.now().plusSeconds(600), chiave());
        String user = jwt("USER", Instant.now().plusSeconds(600), chiave());
        String senzaRuolo = jwt(null, Instant.now().plusSeconds(600), chiave());
        String minuscolo = jwt("admin", Instant.now().plusSeconds(600), chiave());
        String scaduto = jwt("ADMIN", Instant.now().minusSeconds(600), chiave());
        String firmaErrata = jwt("ADMIN", Instant.now().plusSeconds(600),
                Keys.hmacShaKeyFor("un-segreto-completamente-diverso-0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        // payload sostituito con ruolo ADMIN, firma del token USER mantenuta
        String[] p = user.split("\\.");
        String manomesso = p[0] + "." + b64("{\"sub\":\"test\",\"role\":\"ADMIN\",\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}") + "." + p[2];
        String algNone = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + b64("{\"sub\":\"test\",\"role\":\"ADMIN\"}") + ".";

        StringBuilder rep = new StringBuilder("endpoint | senza token | USER | senza ruolo | 'admin' minuscolo | scaduto | firma errata | manomesso | alg=none | malformato | ADMIN\n");
        for (Endpoint e : eps) {
            int s0 = status(e.metodo(), e.path(), null);
            int sUser = status(e.metodo(), e.path(), user);
            int sNoRole = status(e.metodo(), e.path(), senzaRuolo);
            int sLower = status(e.metodo(), e.path(), minuscolo);
            int sExp = status(e.metodo(), e.path(), scaduto);
            int sSig = status(e.metodo(), e.path(), firmaErrata);
            int sTam = status(e.metodo(), e.path(), manomesso);
            int sNone = status(e.metodo(), e.path(), algNone);
            int sBad = status(e.metodo(), e.path(), "abc.def");
            int sAdm = status(e.metodo(), e.path(), admin);
            rep.append("%s %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %d%n".formatted(
                    e.metodo(), e.pattern().replace(API, ""), s0, sUser, sNoRole, sLower, sExp, sSig, sTam, sNone, sBad, sAdm));

            assertEquals(401, s0, "senza token " + e);
            assertEquals(403, sUser, "ruolo USER " + e);
            assertEquals(403, sNoRole, "token senza claim role " + e);
            assertEquals(403, sLower, "ruolo 'admin' minuscolo (hasRole è case-sensitive) " + e);
            assertEquals(401, sExp, "token scaduto " + e);
            assertEquals(401, sSig, "firma errata " + e);
            assertEquals(401, sTam, "payload manomesso " + e);
            assertEquals(401, sNone, "alg=none " + e);
            assertEquals(401, sBad, "token malformato " + e);
            assertNotEquals(401, sAdm, "ADMIN valido rifiutato " + e);
            assertNotEquals(403, sAdm, "ADMIN valido senza permessi " + e);
        }
        Files.writeString(Path.of("target", "security-matrix.txt"), rep.toString());
    }

    @Test
    void tokenInQueryStringNonAccettato() throws Exception {
        String admin = jwt("ADMIN", Instant.now().plusSeconds(600), chiave());
        assertEquals(401, status("GET", API + "/admin/dashboard/stats?data=2026-11-18&access_token=" + admin, null));
    }

    @Test
    void variantiDiUrlMaiAutorizzate() throws Exception {
        String base = API;
        List<String[]> varianti = List.of(
                new String[]{"GET", base + "/admin/dashboard/stats/"},
                new String[]{"GET", base + "/admin/dashboard/stats;x=y"},
                new String[]{"GET", base + "/admin/./dashboard/stats"},
                new String[]{"GET", base + "//admin/dashboard/stats"},
                new String[]{"GET", base + "/admin//dashboard/stats"},
                new String[]{"GET", base + "/stack/../admin/dashboard/stats"},
                new String[]{"GET", base + "/stack/%2e%2e/admin/dashboard/stats"},
                new String[]{"GET", base + "/admin%2Fdashboard%2Fstats"},
                new String[]{"GET", base + "/admin/dashboard%2Fstats"},
                new String[]{"GET", base + "/admin/dashboard/stats%00"},
                new String[]{"GET", base + "/admin%5Cdashboard/stats"},
                new String[]{"GET", base + "/ADMIN/dashboard/stats"},
                new String[]{"GET", base + "/Admin/dashboard/stats"},
                new String[]{"GET", base + "/admin/dashboard/stats.json"},
                new String[]{"GET", base + "/admin/dashboard/stats%20"},
                new String[]{"POST", base + "/auth/codifica/"},
                new String[]{"POST", base + "/auth/codifica;x=y"},
                new String[]{"POST", base + "/auth/CODIFICA"},
                new String[]{"POST", base + "/auth/./codifica"},
                new String[]{"POST", base + "//auth/codifica"},
                new String[]{"POST", base + "/auth/codifica%2f"},
                new String[]{"GET", base + "/auth/codifica"},
                new String[]{"PUT", base + "/auth/codifica"},
                new String[]{"DELETE", base + "/auth/codifica"},
                new String[]{"OPTIONS", base + "/admin/dashboard/stats"},
                new String[]{"HEAD", base + "/admin/dashboard/stats"},
                new String[]{"TRACE", base + "/admin/dashboard/stats"});

        StringBuilder rep = new StringBuilder("metodo url | esito | classificazione\n");
        for (String[] v : varianti) {
            int s;
            try {
                s = status(v[0], v[1] + (v[1].contains("?") ? "" : "?data=2026-11-18&password=x"), null);
            } catch (IllegalArgumentException | java.io.IOException ex) {
                rep.append("%s %s | rifiutato dal client di test | %s%n".formatted(v[0], v[1], ex.getClass().getSimpleName()));
                continue;
            }
            String classe = switch (s) {
                case 400 -> "FIREWALL (400: rifiutato prima della catena di sicurezza)";
                case 401 -> "SICUREZZA (401: richiede autenticazione)";
                case 403 -> "SICUREZZA (403)";
                case 404, 405 -> "NESSUN HANDLER (%d)".formatted(s);
                case 500 -> "NESSUN HANDLER (500: catch-all di ExceptionController, nessun dato esposto)";
                default -> "!!! " + s;
            };
            rep.append("%s %s | %d | %s%n".formatted(v[0], v[1].replace(API, ""), s, classe));
            assertTrue(s >= 400, v[0] + " " + v[1] + " -> " + s);
        }
        Files.writeString(Path.of("target", "security-variants.txt"), rep.toString());
    }
}
