package com.skillteam.auth.controller;

import com.skillteam.auth.dto.RegisterRequest;
import com.skillteam.auth.dto.RegisterResponse;
import com.skillteam.auth.service.AuthRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthRegistrationService authRegistrationService;

    public AuthController(AuthRegistrationService authRegistrationService) {
        this.authRegistrationService = authRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
