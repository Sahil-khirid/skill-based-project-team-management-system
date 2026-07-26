package com.skillteam.projectteam.dto;

import com.skillteam.projectteam.entity.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
        @NotNull
        Long authUserId,

        @NotNull
        ProjectMemberRole role
) {
}
