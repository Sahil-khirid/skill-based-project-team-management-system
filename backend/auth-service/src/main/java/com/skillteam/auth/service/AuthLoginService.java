package com.skillteam.auth.service;

import com.skillteam.auth.config.JwtProperties;
import com.skillteam.auth.config.RefreshTokenProperties;
import com.skillteam.auth.dto.AuthenticatedUserResponse;
import com.skillteam.auth.dto.LoginRequest;
import com.skillteam.auth.dto.LoginResponse;
import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.RefreshToken;
import com.skillteam.auth.exception.InvalidCredentialsException;
import com.skillteam.auth.repository.AuthUserRepository;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.AuthUserPrincipal;
import com.skillteam.auth.security.JwtService;
import com.skillteam.auth.security.RefreshTokenGenerator;
import com.skillteam.auth.security.RefreshTokenHasher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthLoginService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password.";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenProperties refreshTokenProperties;
    private final Clock clock;

    public AuthLoginService(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             JwtProperties jwtProperties,
                             RefreshTokenRepository refreshTokenRepository,
                             AuthUserRepository authUserRepository,
                             RefreshTokenGenerator refreshTokenGenerator,
                             RefreshTokenHasher refreshTokenHasher,
                             RefreshTokenProperties refreshTokenProperties,
                             Clock clock) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authUserRepository = authUserRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTokenProperties = refreshTokenProperties;
        this.clock = clock;
    }

    @Transactional
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

        String rawRefreshToken = refreshTokenGenerator.generate();
        String tokenHash = refreshTokenHasher.hash(rawRefreshToken);
        Instant expiresAt = clock.instant().plus(refreshTokenProperties.getTtl());
        AuthUser userReference = authUserRepository.getReferenceById(principal.getId());
        refreshTokenRepository.save(new RefreshToken(userReference, tokenHash, expiresAt));

        return new LoginResponse(accessToken, "Bearer", jwtProperties.getAccessTokenTtl().toSeconds(), user, rawRefreshToken);
    }
}
