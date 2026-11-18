package it.vasilepersonalsite;

import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.service.GoogleMeetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matrice di autorizzazione costruita dagli endpoint REALMENTE registrati:
 * se in futuro si aggiunge un endpoint sotto /admin (o /auth/codifica) senza
 * protezione, questo test fallisce. Verificato sia su Tomcat reale sia su MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test.properties")
class SicurezzaEndpointTest {

    private static final String API = "/luigi/vasile/personal/api";

    /** Endpoint che devono restare pubblici (contratto con il frontend Angular). */
    private static final Set<String> PUBBLICI_ATTESI = Set.of(
            "GET " + API + "/auth/ping",
            "POST " + API + "/auth/login",
            "POST " + API + "/auth/sblocca",
            "GET " + API + "/stack/progetti",
            "GET " + API + "/stack/readme",
            "GET " + API + "/stack/skills",
            "GET " + API + "/stack/skills/{id}",
            "GET " + API + "/stack/categories",
            "GET " + API + "/stack/keywords",
            "POST " + API + "/lezioni",
            "PUT " + API + "/lezioni/modifica",
            "PUT " + API + "/lezioni/annulla",
            "GET " + API + "/lezioni/settimana"
    );

    @Autowired TestRestTemplate http;
    @Autowired MockMvc mvc;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @MockBean EmailService emailService;
    @MockBean GoogleMeetService googleMeetService;

    private record Endpoint(String metodo, String pattern) {
        String concreto() {
            return pattern.replace("{id}", "1");
        }
    }

    private List<Endpoint> endpointApplicativi() {
        List<Endpoint> out = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("it.vasilepersonalsite")) {
                return;
            }
            for (String p : info.getPatternValues()) {
                for (RequestMethod m : info.getMethodsCondition().getMethods()) {
                    out.add(new Endpoint(m.name(), p));
                }
            }
        });
        return out;
    }

    private List<Endpoint> riservati() {
        return endpointApplicativi().stream()
                .filter(e -> !PUBBLICI_ATTESI.contains(e.metodo() + " " + e.pattern()))
                .toList();
    }

    @Test
    void elencoEndpointCompleto() {
        List<Endpoint> tutti = endpointApplicativi();
        // ogni endpoint pubblico atteso esiste davvero (evita una whitelist obsoleta)
        for (String atteso : PUBBLICI_ATTESI) {
            assertTrue(tutti.stream().anyMatch(e -> (e.metodo() + " " + e.pattern()).equals(atteso)), atteso);
        }
        // ogni endpoint non pubblico è sotto /admin oppure è /auth/codifica
        for (Endpoint e : riservati()) {
            assertTrue(e.pattern().startsWith(API + "/admin/") || e.pattern().equals(API + "/auth/codifica"),
                    "endpoint non classificato: " + e);
        }
        assertTrue(riservati().size() >= 15, "attesi almeno 15 endpoint riservati, trovati " + riservati().size());
    }

    @Test
    void endpointRiservatiRispondono401SenzaTokenSuTomcat() {
        for (Endpoint e : riservati()) {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> r = http.exchange(e.concreto() + "?data=2026-11-18&id=x&password=x",
                    HttpMethod.valueOf(e.metodo()), new HttpEntity<>("{}", h), String.class);
            assertEquals(401, r.getStatusCode().value(), "Tomcat " + e);
        }
    }

    @Test
    void endpointRiservatiRispondono401SenzaTokenSuMockMvc() throws Exception {
        for (Endpoint e : riservati()) {
            mvc.perform(request(HttpMethod.valueOf(e.metodo()), e.concreto())
                            .param("data", "2026-11-18").param("id", "x").param("password", "x")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void varianteDiPathNonAggiranoLaProtezione() {
        for (String path : List.of(
                API + "/admin/dashboard/stats/",
                API + "/admin/dashboard/stats;x=y",
                API + "/admin/./dashboard/stats",
                API + "/auth/codifica/")) {
            ResponseEntity<String> r = http.exchange(path + "?data=2026-11-18&password=x",
                    path.contains("codifica") ? HttpMethod.POST : HttpMethod.GET, HttpEntity.EMPTY, String.class);
            int code = r.getStatusCode().value();
            // requisito di sicurezza: MAI una risposta 2xx/3xx. 401 = bloccato da security,
            // 400 = rifiutato dal firewall; 500 = path inesistente gestito dal catch-all
            // dell'ExceptionController (comportamento già presente nella baseline, nessun dato esposto)
            assertTrue(code >= 400, path + " -> " + code);
            assertFalse(r.getBody() != null && r.getBody().startsWith("$2"), path + " ha restituito un hash");
        }
    }

    @Test
    void endpointPubbliciRestanoPubblici() {
        assertEquals(200, http.getForEntity(API + "/auth/ping", String.class).getStatusCode().value());
        assertEquals(200, http.getForEntity(API + "/stack/skills", String.class).getStatusCode().value());
        assertEquals(200, http.getForEntity(API + "/stack/categories", String.class).getStatusCode().value());
        assertEquals(200, http.getForEntity(API + "/stack/keywords", String.class).getStatusCode().value());
        assertEquals(200, http.getForEntity(API + "/lezioni/settimana?data=2026-11-18", String.class).getStatusCode().value());

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        // login e sblocco raggiungono il controller (non 401 "Autenticazione richiesta" da security)
        ResponseEntity<String> login = http.exchange(API + "/auth/login", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"nessuno\",\"password\":\"x\"}", h), String.class);
        assertEquals(401, login.getStatusCode().value());
        assertTrue(login.getBody().contains("Credenziali non valide"));

        ResponseEntity<String> sblocca = http.exchange(API + "/auth/sblocca", HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"nessuno\",\"codice\":\"X\"}", h), String.class);
        assertEquals(400, sblocca.getStatusCode().value());

        // lezioni pubbliche: arrivano alla validazione applicativa
        ResponseEntity<String> crea = http.exchange(API + "/lezioni", HttpMethod.POST,
                new HttpEntity<>("{\"materia\":\"Analisi\"}", h), String.class);
        assertEquals(400, crea.getStatusCode().value());
    }
}
