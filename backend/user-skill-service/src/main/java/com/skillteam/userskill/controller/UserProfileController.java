package com.skillteam.userskill.controller;

import com.skillteam.userskill.dto.UserProfileRequest;
import com.skillteam.userskill.dto.UserProfileResponse;
import com.skillteam.userskill.security.IdentityHeaderResolver;
import com.skillteam.userskill.service.UserProfileService;
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
@RequestMapping("/api/v1/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final IdentityHeaderResolver identityHeaderResolver;

    public UserProfileController(UserProfileService userProfileService, IdentityHeaderResolver identityHeaderResolver) {
        this.userProfileService = userProfileService;
        this.identityHeaderResolver = identityHeaderResolver;
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> create(HttpServletRequest httpRequest,
                                                        @Valid @RequestBody UserProfileRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        UserProfileResponse response = userProfileService.create(authUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> get(HttpServletRequest httpRequest) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        return ResponseEntity.ok(userProfileService.get(authUserId));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> update(HttpServletRequest httpRequest,
                                                        @Valid @RequestBody UserProfileRequest request) {
        Long authUserId = identityHeaderResolver.resolve(httpRequest);
        return ResponseEntity.ok(userProfileService.update(authUserId, request));
    }
}
