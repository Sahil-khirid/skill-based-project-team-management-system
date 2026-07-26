package com.skillteam.projectteam.controller;

import com.skillteam.projectteam.dto.AddProjectMemberRequest;
import com.skillteam.projectteam.dto.ProjectMemberResponse;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import com.skillteam.projectteam.service.ProjectMemberService;
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
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public ProjectMemberController(ProjectMemberService projectMemberService,
                                    IdentityHeaderResolver identityHeaderResolver) {
        this.projectMemberService = projectMemberService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> create(HttpServletRequest httpRequest,
                                                         @PathVariable Long projectId,
                                                         @Valid @RequestBody AddProjectMemberRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        ProjectMemberResponse response = projectMemberService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> list(HttpServletRequest httpRequest,
                                                             @PathVariable Long projectId) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(projectMemberService.list(projectId));
    }

    @DeleteMapping("/{authUserId}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest,
                                        @PathVariable Long projectId,
                                        @PathVariable Long authUserId) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        projectMemberService.delete(projectId, authUserId);
        return ResponseEntity.noContent().build();
    }
}
