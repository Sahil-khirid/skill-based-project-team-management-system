package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.AddProjectRequiredSkillRequest;
import com.skillteam.projectteam.entity.ProficiencyLevel;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.exception.ProjectRequiredSkillAlreadyExistsException;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
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
class ProjectRequiredSkillServiceTest {

    @Mock
    private ProjectRequiredSkillRepository projectRequiredSkillRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectRequiredSkillService projectRequiredSkillService;

    @Test
    void createForNonexistentProjectIsTranslatedToProjectNotFoundException() {
        AddProjectRequiredSkillRequest request = new AddProjectRequiredSkillRequest(1L, ProficiencyLevel.BEGINNER);

        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectRequiredSkillService.create(999L, request))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("No project exists for this id.");
    }

    @Test
    void concurrentDuplicateRequiredSkillIsTranslatedToProjectRequiredSkillAlreadyExistsException() {
        AddProjectRequiredSkillRequest request = new AddProjectRequiredSkillRequest(1L, ProficiencyLevel.BEGINNER);

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectRequiredSkillRepository.existsByProjectIdAndSkillId(1L, 1L)).thenReturn(false);
        when(projectRequiredSkillRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> projectRequiredSkillService.create(1L, request))
                .isInstanceOf(ProjectRequiredSkillAlreadyExistsException.class)
                .hasMessage("This skill is already required for this project.");
    }

    @Test
    void listForNonexistentProjectIsTranslatedToProjectNotFoundException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectRequiredSkillService.list(999L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("No project exists for this id.");
    }

    @Test
    void deleteForNonexistentProjectIsTranslatedToProjectNotFoundException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectRequiredSkillService.delete(999L, 1L))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("No project exists for this id.");
    }
}
