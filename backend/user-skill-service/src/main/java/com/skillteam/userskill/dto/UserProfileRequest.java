package com.skillteam.userskill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        String displayName,

        @Size(max = 160)
        String headline,

        @Size(max = 500)
        String bio
) {

    public UserProfileRequest {
        displayName = displayName == null ? null : displayName.trim();
        headline = normalizeOptional(headline);
        bio = normalizeOptional(bio);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
