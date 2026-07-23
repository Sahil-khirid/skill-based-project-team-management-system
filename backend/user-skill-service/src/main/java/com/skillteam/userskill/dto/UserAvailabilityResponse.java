package com.skillteam.userskill.dto;

import java.time.Instant;

public record UserAvailabilityResponse(
        Long id,
        int availableHoursPerWeek,
        Instant createdAt,
        Instant updatedAt
) {
}
