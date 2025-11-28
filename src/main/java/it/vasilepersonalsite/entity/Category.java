package it.vasilepersonalsite.entity;


import it.vasilepersonalsite.DTO.CategoryDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @ManyToMany(mappedBy = "categories")
    private Set<Skill> skills = new LinkedHashSet<>();

    public Category(CategoryDto dto) {
        this.id = dto.getId();      // per create sarà null, per update puoi riusarlo
        this.name = dto.getName();

        // difesa nel caso qualcuno usi il costruttore su un oggetto con skills null
        if (this.skills == null) {
            this.skills = new LinkedHashSet<>();
        }
    }

}
