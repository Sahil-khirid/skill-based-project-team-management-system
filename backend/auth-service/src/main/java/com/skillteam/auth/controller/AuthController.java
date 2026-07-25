package com.skillteam.auth.controller;

import com.skillteam.auth.dto.AuthenticatedUserResponse;
import com.skillteam.auth.dto.LoginRequest;
import com.skillteam.auth.dto.LoginResponse;
import com.skillteam.auth.dto.LogoutRequest;
import com.skillteam.auth.dto.RefreshRequest;
import com.skillteam.auth.dto.RefreshResponse;
import com.skillteam.auth.dto.RegisterRequest;
import com.skillteam.auth.dto.RegisterResponse;
import com.skillteam.auth.security.AuthUserPrincipal;
import com.skillteam.auth.service.AuthLoginService;
import com.skillteam.auth.service.AuthLogoutService;
import com.skillteam.auth.service.AuthRefreshService;
import com.skillteam.auth.service.AuthRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthRegistrationService authRegistrationService;
    private final AuthLoginService authLoginService;
    private final AuthRefreshService authRefreshService;
    private final AuthLogoutService authLogoutService;

    public AuthController(AuthRegistrationService authRegistrationService,
                           AuthLoginService authLoginService,
                           AuthRefreshService authRefreshService,
                           AuthLogoutService authLogoutService) {
        this.authRegistrationService = authRegistrationService;
        this.authLoginService = authLoginService;
        this.authRefreshService = authRefreshService;
        this.authLogoutService = authLogoutService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authLoginService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(@AuthenticationPrincipal AuthUserPrincipal principal) {
        AuthenticatedUserResponse response =
                new AuthenticatedUserResponse(principal.getId(), principal.getEmail(), principal.getRole());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResponse response = authRefreshService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authLogoutService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
