package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeywordDao extends JpaRepository<Keyword,Long> {
    Optional<Keyword> findByValueIgnoreCase(String value);
}
