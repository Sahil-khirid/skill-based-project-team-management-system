package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.CreateProjectRequest;
import com.skillteam.projectteam.exception.ProjectAlreadyExistsException;
import com.skillteam.projectteam.repository.ProjectRepository;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void concurrentDuplicateCreationIsTranslatedToProjectAlreadyExistsException() {
        CreateProjectRequest request = new CreateProjectRequest("Apollo", null);

        when(projectRepository.existsByNormalizedName("apollo")).thenReturn(false);
        when(projectRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> projectService.create(1L, request))
                .isInstanceOf(ProjectAlreadyExistsException.class)
                .hasMessage("A project with this name already exists.");
    }
}
