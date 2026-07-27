package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.CreateProjectRequest;
import com.skillteam.projectteam.entity.Project;
import com.skillteam.projectteam.entity.ProjectStatus;
import com.skillteam.projectteam.exception.ProjectAlreadyExistsException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRequiredSkillRepository projectRequiredSkillRepository;

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

    @Test
    void deleteRemovesMembersThenRequiredSkillsThenTheProjectItself() {
        Project project = new Project("Apollo", "apollo", null, ProjectStatus.PLANNING, 1L);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        projectService.delete(5L);

        InOrder order = inOrder(projectMemberRepository, projectRequiredSkillRepository, projectRepository);
        order.verify(projectMemberRepository).deleteByProjectId(5L);
        order.verify(projectRequiredSkillRepository).deleteByProjectId(5L);
        order.verify(projectRepository).delete(project);
    }
}
