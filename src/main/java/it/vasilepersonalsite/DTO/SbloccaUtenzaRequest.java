package it.vasilepersonalsite.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SbloccaUtenzaRequest {

    @NotBlank(message = "Username obbligatorio")
    private String username;

    @NotBlank(message = "Codice di sblocco obbligatorio")
    private String codice;
}
