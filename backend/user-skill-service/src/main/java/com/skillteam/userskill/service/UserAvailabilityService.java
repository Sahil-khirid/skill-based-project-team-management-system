package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserAvailabilityRequest;
import com.skillteam.userskill.dto.UserAvailabilityResponse;
import com.skillteam.userskill.entity.UserAvailability;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.exception.ProfileNotFoundException;
import com.skillteam.userskill.exception.UserAvailabilityAlreadyExistsException;
import com.skillteam.userskill.exception.UserAvailabilityNotFoundException;
import com.skillteam.userskill.repository.UserAvailabilityRepository;
import com.skillteam.userskill.repository.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserAvailabilityService {

    private static final String PROFILE_NOT_FOUND_MESSAGE = "No profile exists for this user.";
    private static final String ALREADY_EXISTS_MESSAGE = "Availability already exists for this user.";
    private static final String NOT_FOUND_MESSAGE = "No availability exists for this user.";

    private final UserAvailabilityRepository userAvailabilityRepository;
    private final UserProfileRepository userProfileRepository;

    public UserAvailabilityService(UserAvailabilityRepository userAvailabilityRepository,
                                    UserProfileRepository userProfileRepository) {
        this.userAvailabilityRepository = userAvailabilityRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserAvailabilityResponse create(Long authUserId, UserAvailabilityRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ProfileNotFoundException(PROFILE_NOT_FOUND_MESSAGE));

        if (userAvailabilityRepository.existsByUserProfileId(profile.getId())) {
            throw new UserAvailabilityAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        UserAvailability availability = new UserAvailability(profile.getId(), request.availableHoursPerWeek());

        UserAvailability saved;
        try {
            saved = userAvailabilityRepository.saveAndFlush(availability);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAvailabilityAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserAvailabilityResponse get(Long authUserId) {
        UserAvailability availability = findByAuthUserIdOrThrow(authUserId);
        return toResponse(availability);
    }

    @Transactional
    public UserAvailabilityResponse update(Long authUserId, UserAvailabilityRequest request) {
        UserAvailability availability = findByAuthUserIdOrThrow(authUserId);

        availability.setAvailableHoursPerWeek(request.availableHoursPerWeek());
        UserAvailability saved = userAvailabilityRepository.saveAndFlush(availability);

        return toResponse(saved);
    }

    private UserAvailability findByAuthUserIdOrThrow(Long authUserId) {
        Optional<UserProfile> profile = userProfileRepository.findByAuthUserId(authUserId);
        if (profile.isEmpty()) {
            throw new UserAvailabilityNotFoundException(NOT_FOUND_MESSAGE);
        }

        return userAvailabilityRepository.findByUserProfileId(profile.get().getId())
                .orElseThrow(() -> new UserAvailabilityNotFoundException(NOT_FOUND_MESSAGE));
    }

    private UserAvailabilityResponse toResponse(UserAvailability availability) {
        return new UserAvailabilityResponse(
                availability.getId(),
                availability.getAvailableHoursPerWeek(),
                availability.getCreatedAt(),
                availability.getUpdatedAt()
        );
    }
}
