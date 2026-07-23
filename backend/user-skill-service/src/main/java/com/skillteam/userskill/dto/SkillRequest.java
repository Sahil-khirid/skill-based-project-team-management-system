package com.skillteam.userskill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkillRequest(

        @NotBlank
        @Size(min = 1, max = 100)
        String name,

        @Size(max = 500)
        String description
) {

    public SkillRequest {
        name = normalizeWhitespace(name);
        description = normalizeOptional(description);
    }

    private static String normalizeWhitespace(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
