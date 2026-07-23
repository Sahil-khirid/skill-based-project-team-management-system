package com.skillteam.userskill.controller;

import com.skillteam.userskill.dto.UserAvailabilityRequest;
import com.skillteam.userskill.dto.UserAvailabilityResponse;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import com.skillteam.userskill.service.UserAvailabilityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/availability")
public class UserAvailabilityController {

    private final UserAvailabilityService userAvailabilityService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public UserAvailabilityController(UserAvailabilityService userAvailabilityService,
                                       IdentityHeaderResolver identityHeaderResolver) {
        this.userAvailabilityService = userAvailabilityService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<UserAvailabilityResponse> create(HttpServletRequest httpRequest,
                                                             @Valid @RequestBody UserAvailabilityRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        UserAvailabilityResponse response = userAvailabilityService.create(authUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<UserAvailabilityResponse> get(HttpServletRequest httpRequest) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(userAvailabilityService.get(authUserId));
    }

    @PutMapping
    public ResponseEntity<UserAvailabilityResponse> update(HttpServletRequest httpRequest,
                                                             @Valid @RequestBody UserAvailabilityRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        identityHeaderResolver.resolveRole(httpRequest);

        return ResponseEntity.ok(userAvailabilityService.update(authUserId, request));
    }
}
