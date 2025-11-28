package it.vasilepersonalsite.DAO;

import it.vasilepersonalsite.entity.Skill;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillDao extends JpaRepository<Skill,Long> {

    @EntityGraph(attributePaths = {"categories", "keywords"})
    @Query("select distinct s from Skill s order by s.level desc, s.name asc")
    List<Skill> findAllWithCategoriesAndKeywords();
}
