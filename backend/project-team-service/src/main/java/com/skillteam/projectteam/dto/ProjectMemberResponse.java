package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProjectMemberRole;

import java.time.Instant;

public record ProjectMemberResponse(
        Long id,
        Long projectId,
        Long authUserId,
        ProjectMemberRole role,
        Instant createdAt,
        Instant updatedAt
) {
}
