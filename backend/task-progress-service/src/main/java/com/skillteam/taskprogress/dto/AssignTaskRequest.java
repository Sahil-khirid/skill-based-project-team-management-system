package com.skillteam.taskprogress.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(

        @NotNull
        Long assignedAuthUserId
) {
}
