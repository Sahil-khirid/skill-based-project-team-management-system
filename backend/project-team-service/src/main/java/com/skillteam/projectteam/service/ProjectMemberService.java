package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.AddProjectMemberRequest;
import com.skillteam.projectteam.dto.ProjectMemberResponse;
import com.skillteam.projectteam.entity.ProjectMember;
import com.skillteam.projectteam.exception.ProjectMemberAlreadyExistsException;
import com.skillteam.projectteam.exception.ProjectMemberNotFoundException;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
import com.skillteam.projectteam.repository.ProjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectMemberService {

    private static final String PROJECT_NOT_FOUND_MESSAGE = "No project exists for this id.";
    private static final String ALREADY_EXISTS_MESSAGE = "This user is already a member of this project.";
    private static final String NOT_FOUND_MESSAGE = "No project member exists for this project and user.";

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;

    public ProjectMemberService(ProjectMemberRepository projectMemberRepository, ProjectRepository projectRepository) {
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectMemberResponse create(Long projectId, AddProjectMemberRequest request) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(PROJECT_NOT_FOUND_MESSAGE);
        }

        if (projectMemberRepository.existsByProjectIdAndAuthUserId(projectId, request.authUserId())) {
            throw new ProjectMemberAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        ProjectMember member = new ProjectMember(projectId, request.authUserId(), request.role());

        ProjectMember saved;
        try {
            saved = projectMemberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            throw new ProjectMemberAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(PROJECT_NOT_FOUND_MESSAGE);
        }

        return projectMemberRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long projectId, Long authUserId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndAuthUserId(projectId, authUserId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(NOT_FOUND_MESSAGE));

        projectMemberRepository.delete(member);
    }

    private ProjectMemberResponse toResponse(ProjectMember member) {
        return new ProjectMemberResponse(
                member.getId(),
                member.getProjectId(),
                member.getAuthUserId(),
                member.getRole(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
