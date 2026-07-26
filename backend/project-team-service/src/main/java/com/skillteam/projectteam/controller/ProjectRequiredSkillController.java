package com.skillteam.projectteam.controller;

import com.skillteam.projectteam.dto.AddProjectRequiredSkillRequest;
import com.skillteam.projectteam.dto.ProjectRequiredSkillResponse;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import com.skillteam.projectteam.service.ProjectRequiredSkillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/required-skills")
public class ProjectRequiredSkillController {

    private final ProjectRequiredSkillService projectRequiredSkillService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public ProjectRequiredSkillController(ProjectRequiredSkillService projectRequiredSkillService,
                                           IdentityHeaderResolver identityHeaderResolver) {
        this.projectRequiredSkillService = projectRequiredSkillService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<ProjectRequiredSkillResponse> create(HttpServletRequest httpRequest,
                                                                @PathVariable Long projectId,
                                                                @Valid @RequestBody AddProjectRequiredSkillRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        ProjectRequiredSkillResponse response = projectRequiredSkillService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectRequiredSkillResponse>> list(HttpServletRequest httpRequest,
                                                                    @PathVariable Long projectId) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(projectRequiredSkillService.list(projectId));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest,
                                        @PathVariable Long projectId,
                                        @PathVariable Long skillId) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        projectRequiredSkillService.delete(projectId, skillId);
        return ResponseEntity.noContent().build();
    }
}
