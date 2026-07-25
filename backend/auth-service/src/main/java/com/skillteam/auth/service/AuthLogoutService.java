package com.skillteam.auth.service;

import com.skillteam.auth.dto.LogoutRequest;
import com.skillteam.auth.entity.RefreshToken;
import com.skillteam.auth.repository.RefreshTokenRepository;
import com.skillteam.auth.security.RefreshTokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AuthLogoutService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    public AuthLogoutService(RefreshTokenRepository refreshTokenRepository,
                              RefreshTokenHasher refreshTokenHasher,
                              Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
    }

    /**
     * Always completes successfully regardless of whether the token exists, is expired, or is
     * already revoked — logout must not expose token existence via response differences.
     */
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = refreshTokenHasher.hash(request.refreshToken());

        refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .filter(token -> !token.isRevoked())
                .ifPresent(token -> revoke(token));
    }

    private void revoke(RefreshToken token) {
        token.revoke(clock.instant(), null);
        refreshTokenRepository.save(token);
    }
}
