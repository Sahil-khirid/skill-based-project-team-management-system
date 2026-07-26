package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

public record AddProjectRequiredSkillRequest(
        @NotNull
        Long skillId,

        @NotNull
        ProficiencyLevel proficiencyLevel
) {
}
