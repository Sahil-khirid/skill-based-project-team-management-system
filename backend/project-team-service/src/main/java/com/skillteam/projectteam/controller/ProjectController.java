package com.skillteam.projectteam.controller;

import com.skillteam.projectteam.dto.CreateProjectRequest;
import com.skillteam.projectteam.dto.ProjectResponse;
import com.skillteam.projectteam.dto.UpdateProjectRequest;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import com.skillteam.projectteam.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public ProjectController(ProjectService projectService, IdentityHeaderResolver identityHeaderResolver) {
        this.projectService = projectService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(HttpServletRequest httpRequest,
                                                   @Valid @RequestBody CreateProjectRequest request) {
        Long ownerAuthUserId = identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        ProjectResponse response = projectService.create(ownerAuthUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(HttpServletRequest httpRequest) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(projectService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(projectService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(HttpServletRequest httpRequest, @PathVariable Long id,
                                                   @Valid @RequestBody UpdateProjectRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
