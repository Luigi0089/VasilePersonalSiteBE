package it.vasilepersonalsite.exception.ErrorRespons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private String code;
    private String message;
    private List<FieldValidationError> errors;
}
