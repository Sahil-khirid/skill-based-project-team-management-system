package com.skillteam.userskill.controller;

import com.skillteam.userskill.dto.UserSkillResponse;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import com.skillteam.userskill.service.UserSkillService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal lookup endpoint intended for consumption by other backend services (e.g. the
 * Project & Team Service's recommendation feature), not by end-user clients directly. Unlike
 * {@link UserSkillController}, the target user is identified by a path variable rather than the
 * caller's own identity header. Restricted to PROJECT_MANAGER at this layer; Gateway-level
 * restriction of this route is handled in a later milestone.
 */
@RestController
@RequestMapping("/api/v1/users/{authUserId}/skills")
public class InternalUserSkillController {

    private final UserSkillService userSkillService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public InternalUserSkillController(UserSkillService userSkillService,
                                        IdentityHeaderResolver identityHeaderResolver) {
        this.userSkillService = userSkillService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @GetMapping
    public ResponseEntity<List<UserSkillResponse>> list(HttpServletRequest httpRequest,
                                                          @PathVariable Long authUserId) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(userSkillService.list(authUserId));
    }
}
