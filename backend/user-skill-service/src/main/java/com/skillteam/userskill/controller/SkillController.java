package com.skillteam.userskill.controller;

import com.skillteam.userskill.dto.SkillRequest;
import com.skillteam.userskill.dto.SkillResponse;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import com.skillteam.userskill.service.SkillService;
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
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public SkillController(SkillService skillService, IdentityHeaderResolver identityHeaderResolver) {
        this.skillService = skillService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<SkillResponse> create(HttpServletRequest httpRequest,
                                                 @Valid @RequestBody SkillRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        SkillResponse response = skillService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SkillResponse>> list(HttpServletRequest httpRequest) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(skillService.listActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillResponse> get(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(skillService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillResponse> update(HttpServletRequest httpRequest, @PathVariable Long id,
                                                 @Valid @RequestBody SkillRequest request) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        return ResponseEntity.ok(skillService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(HttpServletRequest httpRequest, @PathVariable Long id) {
        identityHeaderResolver.resolve(httpRequest);
        String role = identityHeaderResolver.resolveRole(httpRequest);
        identityHeaderResolver.requireProjectManager(role);

        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
