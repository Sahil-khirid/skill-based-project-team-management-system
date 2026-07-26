package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProjectStatus;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        ProjectStatus status,
        Long ownerAuthUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
