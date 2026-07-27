package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProficiencyLevel;

public record SkillMatchDetail(
        Long skillId,
        String skillName,
        ProficiencyLevel requiredLevel,
        ProficiencyLevel memberLevel
) {
}
