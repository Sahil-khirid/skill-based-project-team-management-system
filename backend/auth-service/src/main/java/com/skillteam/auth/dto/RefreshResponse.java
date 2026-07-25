package com.skillteam.auth.dto;

public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
}
