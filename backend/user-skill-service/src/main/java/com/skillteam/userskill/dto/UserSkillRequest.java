package com.skillteam.userskill.dto;

import com.skillteam.userskill.entity.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;

public record UserSkillRequest(
        @NotNull
        Long skillId,

        @NotNull
        ProficiencyLevel proficiencyLevel
) {
}
