package com.skillteam.projectteam.controller;

import com.skillteam.projectteam.dto.ProjectRecommendationResponse;
import com.skillteam.projectteam.security.IdentityHeaderResolver;
import com.skillteam.projectteam.service.ProjectRecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/member-recommendations")
public class ProjectRecommendationController {

    private final ProjectRecommendationService projectRecommendationService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public ProjectRecommendationController(ProjectRecommendationService projectRecommendationService,
                                            IdentityHeaderResolver identityHeaderResolver) {
        this.projectRecommendationService = projectRecommendationService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @GetMapping
    public ResponseEntity<ProjectRecommendationResponse> recommend(HttpServletRequest httpRequest,
                                                                     @PathVariable Long projectId) {
        Long callerUserId = identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(projectRecommendationService.recommend(projectId, callerUserId, role));
    }
}
