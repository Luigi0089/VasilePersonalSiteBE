package it.vasilepersonalsite.exception.ErrorRespons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationError {

    private String field;
    private String message;
    private String code;
    private Object rejectedValue;

}
