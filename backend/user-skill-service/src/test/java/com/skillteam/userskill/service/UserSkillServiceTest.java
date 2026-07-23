package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserSkillRequest;
import com.skillteam.userskill.entity.ProficiencyLevel;
import com.skillteam.userskill.entity.Skill;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.exception.UserSkillAlreadyExistsException;
import com.skillteam.userskill.repository.SkillRepository;
import com.skillteam.userskill.repository.UserProfileRepository;
import com.skillteam.userskill.repository.UserSkillRepository;
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
class UserSkillServiceTest {

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private UserSkillService userSkillService;

    @Test
    void concurrentDuplicateAssignmentIsTranslatedToUserSkillAlreadyExistsException() {
        UserProfile profile = new UserProfile(1L, "Test User", null, null);
        Skill skill = new Skill("Java", "java", null);
        UserSkillRequest request = new UserSkillRequest(1L, ProficiencyLevel.BEGINNER);

        when(userProfileRepository.findByAuthUserId(1L)).thenReturn(Optional.of(profile));
        when(skillRepository.findByIdAndActiveTrue(any())).thenReturn(Optional.of(skill));
        when(userSkillRepository.existsByUserProfileIdAndSkillId(any(), any())).thenReturn(false);
        when(userSkillRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> userSkillService.create(1L, request))
                .isInstanceOf(UserSkillAlreadyExistsException.class)
                .hasMessage("This skill is already assigned to this user.");
    }
}
