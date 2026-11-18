package it.vasilepersonalsite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.vasilepersonalsite.DAO.CodiceSbloccoUtenteDao;
import it.vasilepersonalsite.DAO.LezioneDao;
import it.vasilepersonalsite.DAO.UtenteDao;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.entity.Utente;
import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.service.GoogleMeetService;
import it.vasilepersonalsite.util.SimpleAES;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test di regressione dei comportamenti critici, eseguiti contro H2 in memoria.
 * Email e Google Meet sono mock: nessuna chiamata reale a servizi esterni.
 * Nessun dato reale: solo valori fittizi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test.properties")
class RegressioneApiTest {

    private static final String API = "/luigi/vasile/personal/api";

    @Autowired MockMvc mvc;
    /*
     * I controlli di autorizzazione passano da Tomcat reale: i pattern di SecurityConfig
     * non hanno lo "/" iniziale e vengono valutati correttamente solo dal servlet container
     * reale (in MockMvc risultano non applicati). Così il test verifica il comportamento di produzione.
     */
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper json;
    @Autowired UtenteDao utenteDao;
    @Autowired LezioneDao lezioneDao;
    @Autowired CodiceSbloccoUtenteDao codiceSbloccoDao;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean EmailService emailService;
    @MockBean GoogleMeetService googleMeetService;

    @Value("${chiave.SimpleAES}") String chiaveAes;
    @Value("${admin.password}") String universalPassword;

    /** Un lunedì futuro, per rispettare le regole di prenotazione. */
    private final LocalDate lunedi = LocalDate.now().plusWeeks(2).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

    @BeforeEach
    void pulisci() {
        lezioneDao.deleteAll();
        codiceSbloccoDao.deleteAll();
        utenteDao.deleteAll();
        reset(emailService, googleMeetService);
    }

    private void creaAdmin(String username, String password) {
        Utente u = new Utente();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRuolo("ADMIN");
        utenteDao.save(u);
    }

    private String login(String username, String password) throws Exception {
        MvcResult r = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String richiestaLezione(LocalDate data, String inizio, String fine, boolean prova) {
        return """
                {"nomeStudente":"Studente Test","materia":"Analisi 1","livello":"Universita",
                 "note":"nota di prova","dataLezione":"%s","orarioInizio":"%s","orarioFine":"%s",
                 "email":"studente@example.invalid","lezioneDiProva":%s}
                """.formatted(data, inizio, fine, prova);
    }

    private JsonNode creaLezione(LocalDate data, String inizio, String fine) throws Exception {
        MvcResult r = mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(data, inizio, fine, false)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    // ---------------------------------------------------------------- sicurezza

    @Test
    void endpointPubbliciAccessibiliSenzaToken() throws Exception {
        mvc.perform(get(API + "/auth/ping")).andExpect(status().isOk()).andExpect(content().string("OK"));
        mvc.perform(get(API + "/stack/skills")).andExpect(status().isOk());
        mvc.perform(get(API + "/stack/categories")).andExpect(status().isOk());
        mvc.perform(get(API + "/stack/keywords")).andExpect(status().isOk());
        mvc.perform(get(API + "/lezioni/settimana").param("data", lunedi.toString())).andExpect(status().isOk());
    }

    private ResponseEntity<String> getHttp(String path, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) {
            h.setBearerAuth(token);
        }
        return http.exchange(API + path, HttpMethod.GET, new HttpEntity<>(h), String.class);
    }

    @Test
    void endpointAdminRichiedonoToken() {
        for (String path : List.of(
                "/admin/settimana/confermate?data=" + lunedi,
                "/admin/settimana/da/confermare?data=" + lunedi,
                "/admin/settimana/prova?data=" + lunedi,
                "/admin/dashboard/stats?data=" + lunedi,
                "/admin/conferma?id=x",
                "/admin/rifiuta?id=x")) {
            ResponseEntity<String> r = getHttp(path, null);
            assertEquals(401, r.getStatusCode().value(), path);
            assertTrue(r.getBody().contains("\"status\": 401"), path);
        }
    }

    @Test
    void tokenNonValidoRifiutato() {
        assertEquals(401, getHttp("/admin/dashboard/stats?data=" + lunedi, "token.non.valido").getStatusCode().value());
    }

    @Test
    void ruoloNonAdminRiceve403() throws Exception {
        Utente u = new Utente();
        u.setUsername("utente");
        u.setPassword(passwordEncoder.encode("pwd-utente"));
        u.setRuolo("USER");
        utenteDao.save(u);

        MvcResult r = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"utente\",\"password\":\"pwd-utente\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(r.getResponse().getContentAsString()).get("token").asText();

        ResponseEntity<String> risposta = getHttp("/admin/dashboard/stats?data=" + lunedi, token);
        assertEquals(403, risposta.getStatusCode().value());
        assertTrue(risposta.getBody().contains("\"status\": 403"));
    }

    @Test
    void loginAdminEAccessoDashboard() throws Exception {
        creaAdmin("admin", "pwd-admin");
        String token = login("admin", "pwd-admin");

        ResponseEntity<String> r = getHttp("/admin/dashboard/stats?data=" + lunedi, token);
        assertEquals(200, r.getStatusCode().value());
        JsonNode stats = json.readTree(r.getBody());
        assertEquals(0, stats.get("skills").asLong());
        assertEquals(0, stats.get("lezioniConfermate").asLong());
    }

    @Test
    void codificaPasswordRiservataAdmin() throws Exception {
        // senza token: bloccato
        assertEquals(401, http.postForEntity(API + "/auth/codifica?password=x", null, String.class)
                .getStatusCode().value());

        // con JWT admin: stesso comportamento di prima (hash BCrypt verificabile)
        creaAdmin("admin", "pwd-admin");
        String token = login("admin", "pwd-admin");
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<String> r = http.exchange(API + "/auth/codifica?password=nuova-password",
                HttpMethod.POST, new HttpEntity<>(h), String.class);
        assertEquals(200, r.getStatusCode().value());
        assertTrue(passwordEncoder.matches("nuova-password", r.getBody()));
    }

    // ---------------------------------------------------------------- login / blocco / sblocco

    @Test
    void credenzialiErrateRestituiscono401() throws Exception {
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nessuno\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenziali non valide"));
    }

    @Test
    void bloccoDopoMaxTentativiESbloccoConCodice() throws Exception {
        creaAdmin("admin", "pwd-admin");
        String errata = "{\"username\":\"admin\",\"password\":\"sbagliata\"}";

        // max-tentativi = 3 (test.properties)
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON).content(errata))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON).content(errata))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON).content(errata))
                .andExpect(status().isLocked());

        // anche la password corretta ora viene rifiutata
        mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"pwd-admin\"}"))
                .andExpect(status().isLocked());

        ArgumentCaptor<String> codice = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendNotificaUtenzaBloccata(eq("admin"), eq(3), codice.capture());

        // codice errato
        mvc.perform(post(API + "/auth/sblocca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"codice\":\"ZZZZZZZZ\"}"))
                .andExpect(status().isBadRequest());

        // codice corretto (minuscolo: deve essere accettato)
        mvc.perform(post(API + "/auth/sblocca").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"codice\":\"" + codice.getValue().toLowerCase() + "\"}"))
                .andExpect(status().isOk());

        // codice monouso
        assertTrue(codiceSbloccoDao.findByUsername("admin").isEmpty());
        login("admin", "pwd-admin");
    }

    // ---------------------------------------------------------------- prenotazioni

    @Test
    void logPrenotazioneSenzaDatiPersonaliNeCodice() throws Exception {
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> app =
                new ch.qos.logback.core.read.ListAppender<>();
        app.start();
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger(it.vasilepersonalsite.service.impl.LezioneServiceImpl.class);
        logger.addAppender(app);
        try {
            JsonNode creata = creaLezione(lunedi, "18:00:00", "19:00:00");
            String codice = creata.get("codiceModifica").asText();
            mvc.perform(put(API + "/lezioni/modifica").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"id":"%s","codiceModifica":"%s","materia":"Analisi 1","livello":"Universita",
                             "note":"nota segreta","dataLezione":"%s","orarioInizio":"19:00:00","orarioFine":"20:00:00"}
                            """.formatted(creata.get("id").asText(), codice, lunedi)))
                    .andExpect(status().isOk());

            StringBuilder testo = new StringBuilder();
            app.list.forEach(e -> testo.append(e.getFormattedMessage()).append('\n'));
            String log = testo.toString();
            assertFalse(log.contains(codice), "codice di modifica nei log");
            assertFalse(log.contains("studente@example.invalid"), "email nei log");
            assertFalse(log.contains("Studente Test"), "nome nei log");
            assertFalse(log.contains("nota segreta"), "note nei log");
            assertTrue(log.contains("PRENOTA LEZIONE"), "i log operativi restano");
        } finally {
            logger.detachAppender(app);
        }
    }

    @Test
    void creazioneLezioneCifraIlCodiceENotifica() throws Exception {
        JsonNode r = creaLezione(lunedi, "18:00:00", "19:00:00");

        String codiceInChiaro = r.get("codiceModifica").asText();
        assertEquals(8, codiceInChiaro.length());
        assertEquals("In approvazione", r.get("stato").asText());

        PrenotazioneLezione salvata = lezioneDao.findById(r.get("id").asText()).orElseThrow();
        assertNotEquals(codiceInChiaro, salvata.getCodiceModifica(), "il codice deve essere cifrato a DB");
        assertEquals(codiceInChiaro, SimpleAES.decripta(salvata.getCodiceModifica(), chiaveAes));

        verify(emailService).sendNotificaLezione(any(), eq("studente@example.invalid"), eq(false));
    }

    @Test
    void vistaPubblicaSenzaDatiPersonali() throws Exception {
        creaLezione(lunedi, "18:00:00", "19:00:00");

        mvc.perform(get(API + "/lezioni/settimana").param("data", lunedi.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].materia").value("Analisi 1"))
                .andExpect(jsonPath("$[0].nomeStudente").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].note").doesNotExist())
                .andExpect(jsonPath("$[0].codiceModifica").doesNotExist());
    }

    @Test
    void regoleCalendario() throws Exception {
        LocalDate domenica = lunedi.plusDays(6);
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(domenica, "18:00:00", "19:00:00", false)))
                .andExpect(status().isConflict());

        // feriale fuori fascia 18-21
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "16:00:00", "17:00:00", false)))
                .andExpect(status().isConflict());

        // inizio non antecedente alla fine
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "19:00:00", "18:00:00", false)))
                .andExpect(status().isConflict());

        // lezione di prova diversa da un'ora
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "18:00:00", "20:00:00", true)))
                .andExpect(status().isConflict());

        // sabato: nessun vincolo di fascia
        LocalDate sabato = lunedi.plusDays(5);
        creaLezione(sabato, "10:00:00", "11:30:00");
    }

    @Test
    void sovrapposizioneRestituisceFasceDisponibili() throws Exception {
        creaLezione(lunedi, "18:00:00", "19:00:00");

        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "18:30:00", "19:30:00", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fasceDisponibili[0].orarioInizio").value("19:00:00"))
                .andExpect(jsonPath("$.fasceDisponibili[0].orarioFine").value("21:00:00"));
    }

    @Test
    void modificaConCodiceCorrettoErrataEUniversale() throws Exception {
        JsonNode creata = creaLezione(lunedi, "18:00:00", "19:00:00");
        String id = creata.get("id").asText();
        String codice = creata.get("codiceModifica").asText();

        String modifica = """
                {"id":"%s","codiceModifica":"%s","materia":"Analisi 2","livello":"Universita",
                 "dataLezione":"%s","orarioInizio":"19:00:00","orarioFine":"20:00:00"}
                """;

        mvc.perform(put(API + "/lezioni/modifica").contentType(MediaType.APPLICATION_JSON)
                        .content(modifica.formatted(id, "SBAGLIATO", lunedi)))
                .andExpect(status().isBadRequest());

        // la propria lezione non va in conflitto con sé stessa; codice case-insensitive
        mvc.perform(put(API + "/lezioni/modifica").contentType(MediaType.APPLICATION_JSON)
                        .content(modifica.formatted(id, codice.toLowerCase(), lunedi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orarioInizio").value("19:00:00"))
                .andExpect(jsonPath("$.codiceModifica").value(codice));

        mvc.perform(put(API + "/lezioni/modifica").contentType(MediaType.APPLICATION_JSON)
                        .content(modifica.formatted(id, universalPassword, lunedi)))
                .andExpect(status().isOk());

        verify(emailService, times(2)).sendNotificaLezione(any(), eq("studente@example.invalid"), eq(true));
    }

    @Test
    void annullamentoConCodice() throws Exception {
        JsonNode creata = creaLezione(lunedi, "18:00:00", "19:00:00");
        String body = """
                {"id":"%s","codiceModifica":"%s","materia":"Analisi 1","livello":"Universita",
                 "dataLezione":"%s","orarioInizio":"18:00:00","orarioFine":"19:00:00"}
                """.formatted(creata.get("id").asText(), creata.get("codiceModifica").asText(), lunedi);

        mvc.perform(put(API + "/lezioni/annulla").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertTrue(lezioneDao.findById(creata.get("id").asText()).orElseThrow().isAnnullata());
        // la fascia torna libera
        creaLezione(lunedi, "18:00:00", "19:00:00");
    }

    @Test
    void confermaAdminGeneraMeetEInviaEmail() throws Exception {
        creaAdmin("admin", "pwd-admin");
        String token = login("admin", "pwd-admin");
        String id = creaLezione(lunedi, "18:00:00", "19:00:00").get("id").asText();
        when(googleMeetService.creaLinkMeet(any())).thenReturn("https://meet.google.com/aaa-bbbb-ccc");

        assertEquals(200, getHttp("/admin/conferma?id=" + id, token).getStatusCode().value());

        PrenotazioneLezione l = lezioneDao.findById(id).orElseThrow();
        assertEquals("Confermata", l.getStato());
        assertEquals("https://meet.google.com/aaa-bbbb-ccc", l.getLinkMeet());
        verify(emailService).confermaLezione(any());

        // seconda conferma: il link esistente viene riusato
        assertEquals(200, getHttp("/admin/conferma?id=" + id, token).getStatusCode().value());
        verify(googleMeetService, times(1)).creaLinkMeet(any());

        ResponseEntity<String> r = getHttp("/admin/settimana/confermate?data=" + lunedi, token);
        assertEquals(200, r.getStatusCode().value());
        JsonNode lista = json.readTree(r.getBody());
        assertEquals("https://meet.google.com/aaa-bbbb-ccc", lista.get(0).get("linkMeet").asText());
        assertEquals("studente@example.invalid", lista.get(0).get("email").asText());
    }

    /*
     * Scenario "timeout di Google durante la conferma": il link non viene creato, la lezione
     * risulta comunque confermata una sola volta, e un secondo click dell'admin (retry manuale)
     * NON crea una seconda prenotazione e ritenta il link con lo stesso requestId (id lezione).
     */
    @Test
    void retryConfermaDopoTimeoutGoogleNonDuplica() throws Exception {
        creaAdmin("admin", "pwd-admin");
        String token = login("admin", "pwd-admin");
        String id = creaLezione(lunedi, "18:00:00", "19:00:00").get("id").asText();
        when(googleMeetService.creaLinkMeet(any()))
                .thenReturn(null)                                   // 1° tentativo: timeout → null
                .thenReturn("https://meet.google.com/aaa-bbbb-ccc"); // 2° tentativo: ok

        assertEquals(200, getHttp("/admin/conferma?id=" + id, token).getStatusCode().value());
        assertNull(lezioneDao.findById(id).orElseThrow().getLinkMeet());
        assertEquals("Confermata", lezioneDao.findById(id).orElseThrow().getStato());

        assertEquals(200, getHttp("/admin/conferma?id=" + id, token).getStatusCode().value());
        assertEquals("https://meet.google.com/aaa-bbbb-ccc", lezioneDao.findById(id).orElseThrow().getLinkMeet());

        // terzo click: link già presente → Google NON viene richiamato
        assertEquals(200, getHttp("/admin/conferma?id=" + id, token).getStatusCode().value());
        ArgumentCaptor<PrenotazioneLezione> l = ArgumentCaptor.forClass(PrenotazioneLezione.class);
        verify(googleMeetService, times(2)).creaLinkMeet(l.capture());
        assertTrue(l.getAllValues().stream().allMatch(x -> id.equals(x.getId())), "stesso requestId Meet");
        assertEquals(1, lezioneDao.count(), "nessuna prenotazione duplicata");
    }

    /* Un doppio invio dello stesso modulo di prenotazione non crea due lezioni sovrapposte. */
    @Test
    void doppioInvioPrenotazioneRifiutatoComeConflitto() throws Exception {
        creaLezione(lunedi, "18:00:00", "19:00:00");
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "18:00:00", "19:00:00", false)))
                .andExpect(status().isConflict());
        assertEquals(1, lezioneDao.count());
        verify(emailService, times(1)).sendNotificaLezione(any(), any(), eq(false));
    }

    @Test
    void validazioneProfanityECampiObbligatori() throws Exception {
        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content(richiestaLezione(lunedi, "18:00:00", "19:00:00", false)
                                .replace("Analisi 1", "cazzo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROFANITY_DETECTED"));

        mvc.perform(post(API + "/lezioni").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materia\":\"Analisi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
