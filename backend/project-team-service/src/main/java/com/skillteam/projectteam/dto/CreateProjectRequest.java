package com.skillteam.projectteam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank
        @Size(min = 2, max = 150)
        String name,

        @Size(max = 500)
        String description
) {

    public CreateProjectRequest {
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
