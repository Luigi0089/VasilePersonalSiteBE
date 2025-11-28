package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryDao extends JpaRepository<Category,Long> {
    Optional<Category> findByNameIgnoreCase(String name);
}
