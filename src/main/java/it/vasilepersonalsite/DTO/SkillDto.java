package it.vasilepersonalsite.DTO;

import it.vasilepersonalsite.entity.Category;
import it.vasilepersonalsite.entity.Keyword;
import it.vasilepersonalsite.entity.Skill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {

    private Long id;

    private String name;

    private Integer level;      // 1..5

    private LocalDate acquired; // oppure String se preferisci

    private String notes;

    /**
     * Nomi delle categorie (es. ["Backend","Linguaggi"])
     */
    private List<String> categories;

    /**
     * Keyword testuali (es. ["Java 8+","OOP",...])
     */
    private List<String> keywords;

    private int years;


    public SkillDto(Skill skill) {
        this.id = skill.getId();
        this.name = skill.getName();
        this.level = skill.getLevel();
        this.acquired = skill.getAcquired();
        // anni di esperienza = differenza in anni tra oggi e data acquired
        if (skill.getAcquired() != null) {
            LocalDate start = skill.getAcquired();
            LocalDate today = LocalDate.now();

            int diffYears = Period.between(start, today).getYears();
            this.years = Math.max(diffYears, 0);   // niente valori negativi
        } else {
            this.years = 0;
        }
        this.notes = skill.getNotes();
        this.categories = skill.getCategories() == null
                ? List.of()
                : skill.getCategories().stream()
                .map(Category::getName)
                .toList();

        this.keywords = skill.getKeywords() == null
                ? List.of()
                : skill.getKeywords().stream()
                .map(Keyword::getValue)
                .toList();

    }

}
