package com.skillteam.taskprogress.dto;

import com.skillteam.taskprogress.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull
        TaskStatus status
) {
}
