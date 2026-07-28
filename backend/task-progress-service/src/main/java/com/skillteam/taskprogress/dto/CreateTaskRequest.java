package com.skillteam.taskprogress.dto;

import com.skillteam.taskprogress.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(

        @NotNull
        Long projectId,

        @NotBlank
        @Size(min = 2, max = 150)
        String title,

        @Size(max = 2000)
        String description,

        @NotNull
        TaskPriority priority,

        LocalDate dueDate
) {

    public CreateTaskRequest {
        title = normalizeWhitespace(title);
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
