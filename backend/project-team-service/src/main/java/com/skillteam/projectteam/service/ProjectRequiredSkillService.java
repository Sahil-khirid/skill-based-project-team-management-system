package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.AddProjectRequiredSkillRequest;
import com.skillteam.projectteam.dto.ProjectRequiredSkillResponse;
import com.skillteam.projectteam.entity.ProjectRequiredSkill;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.exception.ProjectRequiredSkillAlreadyExistsException;
import com.skillteam.projectteam.exception.ProjectRequiredSkillNotFoundException;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectRequiredSkillService {

    private static final String PROJECT_NOT_FOUND_MESSAGE = "No project exists for this id.";
    private static final String ALREADY_EXISTS_MESSAGE = "This skill is already required for this project.";
    private static final String NOT_FOUND_MESSAGE = "No required skill exists for this project and skill.";

    private final ProjectRequiredSkillRepository projectRequiredSkillRepository;
    private final ProjectRepository projectRepository;

    public ProjectRequiredSkillService(ProjectRequiredSkillRepository projectRequiredSkillRepository,
                                        ProjectRepository projectRepository) {
        this.projectRequiredSkillRepository = projectRequiredSkillRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectRequiredSkillResponse create(Long projectId, AddProjectRequiredSkillRequest request) {
        requireProjectExists(projectId);

        if (projectRequiredSkillRepository.existsByProjectIdAndSkillId(projectId, request.skillId())) {
            throw new ProjectRequiredSkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        // skillId is an opaque identifier owned by the User & Skill Service. It is
        // intentionally not validated here — verifying it exists requires a cross-service
        // call, which will be introduced in the Recommendation milestone, not this one.
        ProjectRequiredSkill requiredSkill =
                new ProjectRequiredSkill(projectId, request.skillId(), request.proficiencyLevel());

        ProjectRequiredSkill saved;
        try {
            saved = projectRequiredSkillRepository.saveAndFlush(requiredSkill);
        } catch (DataIntegrityViolationException ex) {
            throw new ProjectRequiredSkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectRequiredSkillResponse> list(Long projectId) {
        requireProjectExists(projectId);

        return projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long projectId, Long skillId) {
        requireProjectExists(projectId);

        ProjectRequiredSkill requiredSkill = projectRequiredSkillRepository
                .findByProjectIdAndSkillId(projectId, skillId)
                .orElseThrow(() -> new ProjectRequiredSkillNotFoundException(NOT_FOUND_MESSAGE));

        projectRequiredSkillRepository.delete(requiredSkill);
    }

    private void requireProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(PROJECT_NOT_FOUND_MESSAGE);
        }
    }

    private ProjectRequiredSkillResponse toResponse(ProjectRequiredSkill requiredSkill) {
        return new ProjectRequiredSkillResponse(
                requiredSkill.getId(),
                requiredSkill.getProjectId(),
                requiredSkill.getSkillId(),
                requiredSkill.getProficiencyLevel(),
                requiredSkill.getCreatedAt(),
                requiredSkill.getUpdatedAt()
        );
    }
}
