package com.skillteam.taskprogress.controller;

import com.skillteam.taskprogress.dto.AssignTaskRequest;
import com.skillteam.taskprogress.dto.CreateTaskRequest;
import com.skillteam.taskprogress.dto.TaskResponse;
import com.skillteam.taskprogress.dto.UpdateTaskProgressRequest;
import com.skillteam.taskprogress.dto.UpdateTaskRequest;
import com.skillteam.taskprogress.dto.UpdateTaskStatusRequest;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import com.skillteam.taskprogress.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public TaskController(TaskService taskService, IdentityHeaderResolver identityHeaderResolver) {
        this.taskService = taskService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(HttpServletRequest httpRequest,
                                                @Valid @RequestBody CreateTaskRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        TaskResponse response = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> list(HttpServletRequest httpRequest) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(taskService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> get(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(taskService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(HttpServletRequest httpRequest, @PathVariable Long id,
                                                @Valid @RequestBody UpdateTaskRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assign(HttpServletRequest httpRequest, @PathVariable Long id,
                                                @Valid @RequestBody AssignTaskRequest request) {
        Long callerUserId = identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(taskService.assign(id, request, callerUserId, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(HttpServletRequest httpRequest, @PathVariable Long id,
                                                      @Valid @RequestBody UpdateTaskStatusRequest request) {
        Long callerUserId = identityHeaderResolver.resolve(httpRequest);
        String callerRole = identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(taskService.updateStatus(id, request, callerUserId, callerRole));
    }

    @PatchMapping("/{id}/progress")
    public ResponseEntity<TaskResponse> updateProgress(HttpServletRequest httpRequest, @PathVariable Long id,
                                                        @Valid @RequestBody UpdateTaskProgressRequest request) {
        Long callerUserId = identityHeaderResolver.resolve(httpRequest);
        String callerRole = identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(taskService.updateProgress(id, request, callerUserId, callerRole));
    }
}
