package com.skillteam.auth.service;

import com.skillteam.auth.config.JwtProperties;
import com.skillteam.auth.dto.AuthenticatedUserResponse;
import com.skillteam.auth.dto.LoginRequest;
import com.skillteam.auth.dto.LoginResponse;
import com.skillteam.auth.exception.InvalidCredentialsException;
import com.skillteam.auth.security.AuthUserPrincipal;
import com.skillteam.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthLoginService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthLoginService(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal.getId(), principal.getEmail(), principal.getRole());
        AuthenticatedUserResponse user = new AuthenticatedUserResponse(principal.getId(), principal.getEmail(), principal.getRole());

        return new LoginResponse(accessToken, "Bearer", jwtProperties.getAccessTokenTtl().toSeconds(), user);
    }
}
