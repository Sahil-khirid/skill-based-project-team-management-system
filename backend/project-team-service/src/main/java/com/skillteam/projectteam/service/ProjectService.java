package com.skillteam.projectteam.service;

import com.skillteam.projectteam.dto.CreateProjectRequest;
import com.skillteam.projectteam.dto.ProjectResponse;
import com.skillteam.projectteam.dto.UpdateProjectRequest;
import com.skillteam.projectteam.entity.Project;
import com.skillteam.projectteam.entity.ProjectStatus;
import com.skillteam.projectteam.exception.ProjectAlreadyExistsException;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.repository.ProjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProjectService {

    private static final String ALREADY_EXISTS_MESSAGE = "A project with this name already exists.";
    private static final String NOT_FOUND_MESSAGE = "No project exists for this id.";

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(Long ownerAuthUserId, CreateProjectRequest request) {
        String normalizedName = normalize(request.name());

        if (projectRepository.existsByNormalizedName(normalizedName)) {
            throw new ProjectAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        Project project = new Project(request.name(), normalizedName, request.description(),
                ProjectStatus.PLANNING, ownerAuthUserId);

        Project saved;
        try {
            saved = projectRepository.saveAndFlush(project);
        } catch (DataIntegrityViolationException ex) {
            throw new ProjectAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByNormalizedNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long id) {
        Project project = findOrThrow(id);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        Project project = findOrThrow(id);

        String normalizedName = normalize(request.name());
        Optional<Project> conflicting = projectRepository.findByNormalizedName(normalizedName);
        if (conflicting.isPresent() && !conflicting.get().getId().equals(id)) {
            throw new ProjectAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        project.setName(request.name());
        project.setNormalizedName(normalizedName);
        project.setDescription(request.description());
        project.setStatus(request.status());

        Project saved;
        try {
            saved = projectRepository.saveAndFlush(project);
        } catch (DataIntegrityViolationException ex) {
            throw new ProjectAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Project project = findOrThrow(id);
        projectRepository.delete(project);
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(NOT_FOUND_MESSAGE));
    }

    private String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getOwnerAuthUserId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
