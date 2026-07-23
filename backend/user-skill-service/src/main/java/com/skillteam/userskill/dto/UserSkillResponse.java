package com.skillteam.userskill.dto;

import com.skillteam.userskill.entity.ProficiencyLevel;

import java.time.Instant;

public record UserSkillResponse(
        Long id,
        Long skillId,
        String skillName,
        ProficiencyLevel proficiencyLevel,
        boolean skillActive,
        Instant createdAt,
        Instant updatedAt
) {
}
