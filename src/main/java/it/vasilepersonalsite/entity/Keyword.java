package it.vasilepersonalsite.entity;


import it.vasilepersonalsite.DTO.KeywordDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "keywords")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "value", nullable = false, length = 150, unique = true)
    private String value;

    @ManyToMany(mappedBy = "keywords")
    private Set<Skill> skills = new LinkedHashSet<>();

    public Keyword(KeywordDto dto) {
        this.id = dto.getId();      // idem: null in create, valorizzato in update se lo usi
        this.value = dto.getValue();

        if (this.skills == null) {
            this.skills = new LinkedHashSet<>();
        }
    }

}
