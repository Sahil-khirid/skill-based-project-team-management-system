package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.SkillRequest;
import com.skillteam.userskill.dto.SkillResponse;
import com.skillteam.userskill.entity.Skill;
import com.skillteam.userskill.exception.SkillAlreadyExistsException;
import com.skillteam.userskill.exception.SkillNotFoundException;
import com.skillteam.userskill.repository.SkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SkillService {

    private static final String ALREADY_EXISTS_MESSAGE = "A skill with this name already exists.";
    private static final String NOT_FOUND_MESSAGE = "No skill exists for this id.";

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String normalizedName = normalize(request.name());

        if (skillRepository.existsByNormalizedName(normalizedName)) {
            throw new SkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        Skill skill = new Skill(request.name(), normalizedName, request.description());

        Skill saved;
        try {
            saved = skillRepository.saveAndFlush(skill);
        } catch (DataIntegrityViolationException ex) {
            throw new SkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listActive() {
        return skillRepository.findByActiveTrueOrderByNormalizedNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse get(Long id) {
        Skill skill = findActiveOrThrow(id);
        return toResponse(skill);
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = findActiveOrThrow(id);

        String normalizedName = normalize(request.name());
        Optional<Skill> conflicting = skillRepository.findByNormalizedName(normalizedName);
        if (conflicting.isPresent() && !conflicting.get().getId().equals(id)) {
            throw new SkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        skill.setName(request.name());
        skill.setNormalizedName(normalizedName);
        skill.setDescription(request.description());

        Skill saved;
        try {
            saved = skillRepository.saveAndFlush(skill);
        } catch (DataIntegrityViolationException ex) {
            throw new SkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = findActiveOrThrow(id);
        skill.setActive(false);
        skillRepository.saveAndFlush(skill);
    }

    private Skill findActiveOrThrow(Long id) {
        return skillRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new SkillNotFoundException(NOT_FOUND_MESSAGE));
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }
}
