package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.CodiceSbloccoUtente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CodiceSbloccoUtenteDao extends JpaRepository<CodiceSbloccoUtente, Long> {

    List<CodiceSbloccoUtente> findByUsername(String username);

    @Transactional
    @Modifying
    long deleteByUsername(String username);
}
