package it.vasilepersonalsite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "codice_sblocco_utente")
@Getter
@Setter
@NoArgsConstructor
public class CodiceSbloccoUtente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /*
     * Hash BCrypt del codice monouso: il codice in chiaro viene
     * solo inviato via mail e non è mai salvato a DB.
     */
    @Column(name = "codice", nullable = false)
    private String codice;

    @Column(name = "data_creazione", nullable = false, updatable = false)
    private Instant dataCreazione = Instant.now();

    public CodiceSbloccoUtente(String username, String codiceHash) {
        this.username = username;
        this.codice = codiceHash;
    }
}
