package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProficiencyLevel;

import java.time.Instant;

public record ProjectRequiredSkillResponse(
        Long id,
        Long projectId,
        Long skillId,
        ProficiencyLevel proficiencyLevel,
        Instant createdAt,
        Instant updatedAt
) {
}
