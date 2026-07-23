package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserProfileRequest;
import com.skillteam.userskill.exception.ProfileAlreadyExistsException;
import com.skillteam.userskill.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void concurrentDuplicateCreationIsTranslatedToProfileAlreadyExistsException() {
        UserProfileRequest request = new UserProfileRequest("Race Condition", null, null);

        when(userProfileRepository.existsByAuthUserId(1L)).thenReturn(false);
        when(userProfileRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userProfileService.create(1L, request))
                .isInstanceOf(ProfileAlreadyExistsException.class)
                .hasMessage("A profile already exists for this user.");
    }
}
