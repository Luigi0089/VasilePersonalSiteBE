package it.vasilepersonalsite.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;
import it.vasilepersonalsite.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class AuthServiceImpl implements AuthService {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.ttl-seconds}")
    private Long jwtTtlSeconds;


    public LoginResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.getUsername())) {
            throw new RuntimeException("Credenziali non valide");
        }

        if (!passwordEncoder.matches(request.getPassword(), adminPassword)) {
            throw new RuntimeException("Credenziali non valide");
        }

        String token = generateToken(adminUsername);

        return new LoginResponse(token);
    }


    private String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtTtlSeconds * 1000);

        return Jwts.builder()
                .subject(username)
                .claim("role", "ADMIN")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
