package it.vasilepersonalsite.exception.ErrorRespons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfanityErrorResponse {

    private String code;
    private String message;
    private String field;
    private String rejectedValue;
}
