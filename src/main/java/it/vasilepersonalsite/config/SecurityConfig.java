package it.vasilepersonalsite.config;

import it.vasilepersonalsite.constans.ApiPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {

        /*
         * Deve utilizzare la stessa conversione usata
         * nell'AuthServiceImpl durante la firma del token.
         */
        byte[] secretBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        SecretKey secretKey = new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        /*
         * Legge questo claim dal JWT:
         *
         * "role": "ADMIN"
         */
        authoritiesConverter.setAuthoritiesClaimName("role");

        /*
         * Trasforma:
         *
         * ADMIN
         *
         * in:
         *
         * ROLE_ADMIN
         */
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        return http

                /*
                 * Applicazione REST stateless:
                 * il token JWT sostituisce la sessione.
                 */
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Login pubblico.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                ApiPath.BASE_PATH + "/auth/login"
                        )
                        .permitAll()

                        /*
                         * Endpoint amministrativi che devono
                         * rimanere pubblici.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                ApiPath.BASE_PATH
                                        + "/"
                                        + ApiPath.ADMIN_PATH
                                        + "/ping",

                                ApiPath.BASE_PATH
                                        + "/"
                                        + ApiPath.ADMIN_PATH
                                        + "/conferma",

                                ApiPath.BASE_PATH
                                        + "/"
                                        + ApiPath.ADMIN_PATH
                                        + "/rifiuta"
                        )
                        .permitAll()

                        /*
                         * Tutti gli altri endpoint sotto /admin/**
                         * richiedono ROLE_ADMIN.
                         */
                        .requestMatchers(
                                ApiPath.BASE_PATH
                                        + "/"
                                        + ApiPath.ADMIN_PATH
                                        + "/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * Tutto il resto del sito rimane pubblico.
                         */
                        .anyRequest()
                        .permitAll()
                )

                /*
                 * Abilita l'autenticazione tramite Bearer JWT
                 * e collega il converter del claim "role".
                 */
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .exceptionHandling(ex -> ex
                        // NON autenticato -> 401
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                {
                                  "status": 401,
                                  "error": "Unauthorized",
                                  "message": "Autenticazione richiesta per accedere a questa risorsa"
                                }
                                """);
                        })
                        // Autenticato ma senza permessi -> 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("""
                                {
                                  "status": 403,
                                  "error": "Forbidden",
                                  "message": "Non hai i permessi per accedere a questa risorsa"
                                }
                                """);
                        })
                )



                .build();
    }
}