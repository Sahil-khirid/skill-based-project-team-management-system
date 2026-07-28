package com.skillteam.taskprogress.controller;

import com.skillteam.taskprogress.dto.TaskProgressSummaryResponse;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import com.skillteam.taskprogress.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/task-progress-summary")
public class ProjectTaskProgressController {

    private final TaskService taskService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public ProjectTaskProgressController(TaskService taskService, IdentityHeaderResolver identityHeaderResolver) {
        this.taskService = taskService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @GetMapping
    public ResponseEntity<TaskProgressSummaryResponse> summary(HttpServletRequest httpRequest,
                                                                @PathVariable Long projectId) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(taskService.getProgressSummary(projectId));
    }
}
