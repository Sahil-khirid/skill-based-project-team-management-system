package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.SkillRequest;
import com.skillteam.userskill.exception.SkillAlreadyExistsException;
import com.skillteam.userskill.repository.SkillRepository;
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
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    @Test
    void concurrentDuplicateCreationIsTranslatedToSkillAlreadyExistsException() {
        SkillRequest request = new SkillRequest("Java", null);

        when(skillRepository.existsByNormalizedName("java")).thenReturn(false);
        when(skillRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> skillService.create(request))
                .isInstanceOf(SkillAlreadyExistsException.class)
                .hasMessage("A skill with this name already exists.");
    }
}
