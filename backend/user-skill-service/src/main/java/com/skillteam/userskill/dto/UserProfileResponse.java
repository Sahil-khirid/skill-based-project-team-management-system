package com.skillteam.userskill.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        Long authUserId,
        String displayName,
        String headline,
        String bio,
        Instant createdAt,
        Instant updatedAt
) {
}
