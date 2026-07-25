package com.skillteam.auth.service;

import com.skillteam.auth.config.JwtProperties;
import com.skillteam.auth.config.RefreshTokenProperties;
import com.skillteam.auth.dto.RefreshRequest;
import com.skillteam.auth.dto.RefreshResponse;
import com.skillteam.auth.entity.AuthUser;
import com.skillteam.auth.entity.RefreshToken;
import com.skillteam.auth.exception.InvalidRefreshTokenException;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.JwtService;
import com.skillteam.auth.security.RefreshTokenGenerator;
import com.skillteam.auth.security.RefreshTokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuthRefreshService {

    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Invalid or expired refresh token.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenProperties refreshTokenProperties;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthRefreshService(RefreshTokenRepository refreshTokenRepository,
                               RefreshTokenGenerator refreshTokenGenerator,
                               RefreshTokenHasher refreshTokenHasher,
                               RefreshTokenProperties refreshTokenProperties,
                               JwtService jwtService,
                               JwtProperties jwtProperties,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTokenProperties = refreshTokenProperties;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String tokenHash = refreshTokenHasher.hash(request.refreshToken());

        RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException(INVALID_REFRESH_TOKEN_MESSAGE));

        Instant now = clock.instant();
        if (currentToken.isRevoked() || currentToken.isExpired(now)) {
            throw new InvalidRefreshTokenException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        AuthUser user = currentToken.getUser();
        if (user == null || !user.isEnabled()) {
            throw new InvalidRefreshTokenException(INVALID_REFRESH_TOKEN_MESSAGE);
        }

        String newRawRefreshToken = refreshTokenGenerator.generate();
        String newTokenHash = refreshTokenHasher.hash(newRawRefreshToken);
        Instant newExpiresAt = now.plus(refreshTokenProperties.getTtl());
        RefreshToken replacementToken = refreshTokenRepository.saveAndFlush(
                new RefreshToken(user, newTokenHash, newExpiresAt));

        currentToken.revoke(now, replacementToken.getId());
        refreshTokenRepository.save(currentToken);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return new RefreshResponse(
                newAccessToken, "Bearer", jwtProperties.getAccessTokenTtl().toSeconds(), newRawRefreshToken);
    }
}
