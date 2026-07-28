package com.skillteam.taskprogress.dto;

public record TaskProgressSummaryResponse(
        Long projectId,
        long totalTasks,
        long todoCount,
        long inProgressCount,
        long completedCount,
        long blockedCount,
        double completionPercentage
) {
}
