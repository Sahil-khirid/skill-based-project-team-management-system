package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.AddProjectMemberRequest;
import com.skillteam.projectteam.entity.ProjectMemberRole;
import com.skillteam.projectteam.exception.ProjectMemberAlreadyExistsException;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
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
class ProjectMemberServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Test
    void createForNonexistentProjectIsTranslatedToProjectNotFoundException() {
        AddProjectMemberRequest request = new AddProjectMemberRequest(1L, ProjectMemberRole.MEMBER);

        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectMemberService.create(999L, request))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("No project exists for this id.");
    }

    @Test
    void concurrentDuplicateAssignmentIsTranslatedToProjectMemberAlreadyExistsException() {
        AddProjectMemberRequest request = new AddProjectMemberRequest(1L, ProjectMemberRole.MEMBER);

        when(projectRepository.existsById(1L)).thenReturn(true);
        when(projectMemberRepository.existsByProjectIdAndAuthUserId(1L, 1L)).thenReturn(false);
        when(projectMemberRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> projectMemberService.create(1L, request))
                .isInstanceOf(ProjectMemberAlreadyExistsException.class)
                .hasMessage("This user is already a member of this project.");
    }
}
