package com.skillteam.userskill.controller;

import com.skillteam.userskill.dto.UserSkillRequest;
import com.skillteam.userskill.dto.UserSkillResponse;
import com.skillteam.userskill.dto.UserSkillUpdateRequest;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import com.skillteam.userskill.service.UserSkillService;
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
@RequestMapping("/api/v1/users/me/skills")
public class UserSkillController {

    private final UserSkillService userSkillService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public UserSkillController(UserSkillService userSkillService, IdentityHeaderResolver identityHeaderResolver) {
        this.userSkillService = userSkillService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<UserSkillResponse> create(HttpServletRequest httpRequest,
                                                      @Valid @RequestBody UserSkillRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        UserSkillResponse response = userSkillService.create(authUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserSkillResponse>> list(HttpServletRequest httpRequest) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(userSkillService.list(authUserId));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<UserSkillResponse> update(HttpServletRequest httpRequest, @PathVariable Long skillId,
                                                      @Valid @RequestBody UserSkillUpdateRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(userSkillService.update(authUserId, skillId, request));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest, @PathVariable Long skillId) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        userSkillService.delete(authUserId, skillId);
        return ResponseEntity.noContent().build();
    }
}
