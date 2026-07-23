package com.skillteam.userskill.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserAvailabilityRequest(
        @NotNull
        @Min(0)
        @Max(168)
        Integer availableHoursPerWeek
) {
}
