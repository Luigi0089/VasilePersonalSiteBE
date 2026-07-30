package it.vasilepersonalsite.controller;

import it.vasilepersonalsite.DTO.LoginRequest;
import it.vasilepersonalsite.DTO.LoginResponse;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.AUTH_PATH)
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/codifica")
    public ResponseEntity<String> login(@RequestParam String password) {
        return ResponseEntity.ok(authService.codificaPassword(password));
    }
}
