package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.Utente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtenteDao extends JpaRepository<Utente, Long> {

    Optional<Utente> findByUsername(String username);

    /*
     * SELECT ... FOR UPDATE: serializza i login concorrenti sulla stessa
     * utenza, così il contatore dei tentativi non perde incrementi e la
     * mail di blocco viene inviata una sola volta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Utente> findWithLockByUsername(@Param("username") String username);
}
