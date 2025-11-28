package it.vasilepersonalsite.entity;

import it.vasilepersonalsite.DTO.SkillDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "acquired")
    private LocalDate acquired;

    @Column(length = 1000)
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "skill_category",
            joinColumns = @JoinColumn(name = "skill_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "skill_keyword",
            joinColumns = @JoinColumn(name = "skill_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    private Set<Keyword> keywords = new LinkedHashSet<>();


    public Skill(SkillDto dto) {
        this.id = dto.getId();                 // attenzione: per create di solito è null
        this.name = dto.getName();
        this.level = dto.getLevel();
        this.acquired = dto.getAcquired();
        this.notes = dto.getNotes();
        // le collezioni le inizializziamo vuote: verranno popolate dal service
        this.categories = new LinkedHashSet<>();
        this.keywords   = new LinkedHashSet<>();
    }


}
