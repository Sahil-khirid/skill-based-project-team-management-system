package com.skillteam.taskprogress.dto;

import com.skillteam.taskprogress.entity.TaskPriority;
import com.skillteam.taskprogress.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Long assignedAuthUserId,
        Integer progressPercentage,
        Instant createdAt,
        Instant updatedAt
) {
}
