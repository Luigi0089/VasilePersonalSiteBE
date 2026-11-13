package it.vasilepersonalsite.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.vasilepersonalsite.DAO.CodiceSbloccoUtenteDao;
import it.vasilepersonalsite.DAO.UtenteDao;
import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;
import it.vasilepersonalsite.DTO.SbloccaUtenzaRequest;
import it.vasilepersonalsite.entity.CodiceSbloccoUtente;
import it.vasilepersonalsite.entity.PrenotazioneLezione;
import it.vasilepersonalsite.entity.Utente;
import it.vasilepersonalsite.exception.NoMatchCodeException;
import it.vasilepersonalsite.exception.PasswordErrataException;
import it.vasilepersonalsite.exception.UtenzaBloccataException;
import it.vasilepersonalsite.service.AuthService;
import it.vasilepersonalsite.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UtenteDao utenteDao;

    @Autowired
    private CodiceSbloccoUtenteDao codiceSbloccoUtenteDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttl-seconds}")
    private Long jwtTtlSeconds;

    /*
     * Numero di login falliti consecutivi dopo i quali l'utenza viene bloccata.
     */
    @Value("${security.login.max-tentativi}")
    private int maxTentativi;

    /*
     * noRollbackFor: l'incremento dei tentativi e il codice di sblocco
     * devono essere salvati anche se il metodo termina con un'eccezione.
     */
    @Override
    @Transactional(noRollbackFor = {PasswordErrataException.class, UtenzaBloccataException.class})
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        log.info("Ricevuta richiesta di autenticazione per username: {}", username);

        Utente utente = utenteDao.findWithLockByUsername(username).orElse(null);

        if (utente == null) {
            log.warn("Autenticazione fallita: username '{}' non trovato (utenti in tabella: {})",
                    username, utenteDao.count());
            throw new PasswordErrataException("Credenziali non valide");
        }

        if (utente.getTentativiLogin() >= maxTentativi) {
            log.warn("Autenticazione rifiutata: utenza {} bloccata ({} tentativi)",
                    username, utente.getTentativiLogin());
            throw new UtenzaBloccataException(username);
        }

        log.debug("Username riconosciuto, avvio verifica password");

        if (!passwordEncoder.matches(request.getPassword(), utente.getPassword())) {
            int tentativi = utente.getTentativiLogin() + 1;
            utente.setTentativiLogin(tentativi);
            utenteDao.save(utente);

            log.warn("Autenticazione fallita: password non valida per username: {} (tentativo {}/{})",
                    username, tentativi, maxTentativi);

            if (tentativi >= maxTentativi) {
                bloccaUtenza(username, tentativi);
                throw new UtenzaBloccataException(username);
            }

            throw new PasswordErrataException("Credenziali non valide");
        }

        if (utente.getTentativiLogin() > 0) {
            utente.setTentativiLogin(0);
            utenteDao.save(utente);
        }

        log.debug("Credenziali valide, generazione token JWT");

        String token = generateToken(utente.getUsername(), utente.getRuolo());

        log.info("Autenticazione completata con successo per username: {} (ruolo {})",
                username, utente.getRuolo());

        return new LoginResponse(token, utente.getUsername(), utente.getRuolo());
    }

    /*
     * Genera il codice monouso, lo salva (hash BCrypt) e lo invia
     * all'indirizzo di notifica. Eventuali codici precedenti vengono
     * invalidati: resta valido solo l'ultimo.
     */
    private void bloccaUtenza(String username, int tentativi) {
        String codice = PrenotazioneLezione.generaCodiceModifica();

        codiceSbloccoUtenteDao.deleteByUsername(username);
        codiceSbloccoUtenteDao.save(
                new CodiceSbloccoUtente(username, passwordEncoder.encode(codice))
        );

        log.warn("Utenza {} bloccata: superato il limite di {} tentativi", username, maxTentativi);

        emailService.sendNotificaUtenzaBloccata(username, tentativi, codice);
    }

    @Override
    @Transactional
    public String sbloccaUtenza(SbloccaUtenzaRequest request) {
        String username = request.getUsername();
        log.info("Richiesta di sblocco per username: {}", username);

        Utente utente = utenteDao.findWithLockByUsername(username)
                .orElseThrow(() -> new NoMatchCodeException("Codice di sblocco non valido"));

        String codice = request.getCodice().trim().toUpperCase();
        List<CodiceSbloccoUtente> codici = codiceSbloccoUtenteDao.findByUsername(username);

        boolean valido = codici.stream()
                .anyMatch(c -> passwordEncoder.matches(codice, c.getCodice()));

        if (!valido) {
            log.warn("Sblocco fallito: codice non valido per username: {}", username);
            throw new NoMatchCodeException("Codice di sblocco non valido");
        }

        utente.setTentativiLogin(0);
        utenteDao.save(utente);

        // codice monouso: dopo l'uso viene eliminato
        codiceSbloccoUtenteDao.deleteByUsername(username);

        log.info("Utenza {} sbloccata correttamente", username);

        return "Utenza " + username + " sbloccata";
    }

    private String generateToken(String username, String ruolo) {
        log.debug("Inizio generazione token JWT per username: {}", username);

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + jwtTtlSeconds * 1000
        );

        String token = Jwts.builder()
                .subject(username)
                .claim("role", ruolo)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(
                        getSigningKey(),
                        Jwts.SIG.HS256
                )
                .compact();

        log.debug(
                "Token JWT generato correttamente per username: {}. Scadenza: {}",
                username,
                expiration
        );

        return token;
    }

    private SecretKey getSigningKey() {
        log.debug("Creazione della chiave di firma JWT");

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String codificaPassword(String password) {
        log.debug("Avvio codifica password tramite PasswordEncoder");

        String encodedPassword = passwordEncoder.encode(password);

        log.debug("Codifica password completata");

        return encodedPassword;
    }
}
