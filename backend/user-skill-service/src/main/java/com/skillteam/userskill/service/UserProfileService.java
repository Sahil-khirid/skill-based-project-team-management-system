package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserProfileRequest;
import com.skillteam.userskill.dto.UserProfileResponse;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.exception.ProfileAlreadyExistsException;
import com.skillteam.userskill.exception.ProfileNotFoundException;
import com.skillteam.userskill.repository.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final String ALREADY_EXISTS_MESSAGE = "A profile already exists for this user.";
    private static final String NOT_FOUND_MESSAGE = "No profile exists for this user.";

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfileResponse create(Long authUserId, UserProfileRequest request) {
        if (userProfileRepository.existsByAuthUserId(authUserId)) {
            throw new ProfileAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        UserProfile profile = new UserProfile(authUserId, request.displayName(), request.headline(), request.bio());

        UserProfile saved;
        try {
            saved = userProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException ex) {
            throw new ProfileAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse get(Long authUserId) {
        UserProfile profile = findByAuthUserIdOrThrow(authUserId);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse update(Long authUserId, UserProfileRequest request) {
        UserProfile profile = findByAuthUserIdOrThrow(authUserId);

        profile.setDisplayName(request.displayName());
        profile.setHeadline(request.headline());
        profile.setBio(request.bio());

        UserProfile saved = userProfileRepository.saveAndFlush(profile);
        return toResponse(saved);
    }

    private UserProfile findByAuthUserIdOrThrow(Long authUserId) {
        return userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ProfileNotFoundException(NOT_FOUND_MESSAGE));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getDisplayName(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
