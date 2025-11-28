package it.vasilepersonalsite.DTO;

import it.vasilepersonalsite.entity.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordDto {

    private Long id;
    private String value;

    public KeywordDto(Keyword keyword) {
        this.id = keyword.getId();
        this.value = keyword.getValue();
    }
}
