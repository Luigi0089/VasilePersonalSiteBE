package it.vasilepersonalsite.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;

    public LoginResponse(String token) {

        this.token = token;
    }

    public LoginResponse(String token, String username) {

        this.token = token;
        this.username = username;

        if( username.toLowerCase().equals("luigi") ){
            this.role = "ADMIN";
        }
    }
}
