package it.vasilepersonalsite.service;

import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;

public interface AuthService {
    public LoginResponse login(LoginRequest request);
}
