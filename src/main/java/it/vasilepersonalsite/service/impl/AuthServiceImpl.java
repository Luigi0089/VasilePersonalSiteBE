package it.vasilepersonalsite.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;
import it.vasilepersonalsite.exception.PasswordErrataException;
import it.vasilepersonalsite.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${admin.username}")
    private String adminUsername;

    /*
     * Deve contenere l'hash BCrypt, non la password in chiaro.
     */
    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttl-seconds}")
    private Long jwtTtlSeconds;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Ricevuta richiesta di autenticazione per username: {}",
                request.getUsername());

        if (!adminUsername.equals(request.getUsername())) {
            log.warn("Autenticazione fallita: username non valido");

            throw new PasswordErrataException("Credenziali non valide");
        }

        log.debug("Username riconosciuto, avvio verifica password");

        if (!passwordEncoder.matches(
                request.getPassword(),
                adminPassword
        )) {
            log.warn(
                    "Autenticazione fallita: password non valida per username: {}",
                    request.getUsername()
            );

            throw new PasswordErrataException("Credenziali non valide");
        }

        log.debug("Credenziali valide, generazione token JWT");

        String token = generateToken(adminUsername);

        log.info("Autenticazione completata con successo per username: {}",
                adminUsername);

        return new LoginResponse(token,request.getUsername());
    }

    private String generateToken(String username) {
        log.debug("Inizio generazione token JWT per username: {}", username);

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + jwtTtlSeconds * 1000
        );

        String token = Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN")
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