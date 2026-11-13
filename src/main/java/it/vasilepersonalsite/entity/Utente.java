package it.vasilepersonalsite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "utente")
@Getter
@Setter
@NoArgsConstructor
public class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /*
     * Hash BCrypt della password, mai la password in chiaro.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /*
     * Ruolo senza prefisso (es. "ADMIN"): finisce nel claim "role" del JWT,
     * e Spring Security lo trasforma in ROLE_ADMIN (vedi SecurityConfig).
     * Il DEFAULT permette a ddl-auto=update di aggiungere la colonna
     * anche se la tabella contiene già righe.
     */
    @Column(name = "ruolo", nullable = false, length = 30,
            columnDefinition = "VARCHAR(30) NOT NULL DEFAULT 'USER'")
    private String ruolo = "USER";

    /*
     * Tentativi di login falliti consecutivi (username corretto, password errata).
     * Si azzera ad ogni login riuscito o allo sblocco dell'utenza.
     */
    @Column(name = "tentativi_login", nullable = false)
    private int tentativiLogin = 0;
}
