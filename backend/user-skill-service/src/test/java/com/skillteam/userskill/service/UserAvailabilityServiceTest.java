package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserAvailabilityRequest;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.exception.UserAvailabilityAlreadyExistsException;
import com.skillteam.userskill.repository.UserAvailabilityRepository;
import com.skillteam.userskill.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAvailabilityServiceTest {

    @Mock
    private UserAvailabilityRepository userAvailabilityRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserAvailabilityService userAvailabilityService;

    @Test
    void concurrentDuplicateCreationIsTranslatedToUserAvailabilityAlreadyExistsException() {
        UserProfile profile = new UserProfile(1L, "Test User", null, null);
        UserAvailabilityRequest request = new UserAvailabilityRequest(40);

        when(userProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.of(profile));
        when(userAvailabilityRepository.existsByUserProfileId(any())).thenReturn(false);
        when(userAvailabilityRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userAvailabilityService.create(1L, request))
                .isInstanceOf(UserAvailabilityAlreadyExistsException.class)
                .hasMessage("Availability already exists for this user.");
    }
}
