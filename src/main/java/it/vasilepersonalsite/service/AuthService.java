package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;
import it.vasilepersonalsite.DTO.SbloccaUtenzaRequest;

public interface AuthService {
     LoginResponse login(LoginRequest request);

     String sbloccaUtenza(SbloccaUtenzaRequest request);

     String codificaPassword(String password);
}
