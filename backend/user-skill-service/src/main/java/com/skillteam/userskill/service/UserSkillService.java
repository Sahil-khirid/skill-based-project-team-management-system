package com.skillteam.userskill.service;

import com.skillteam.userskill.dto.UserSkillRequest;
import com.skillteam.userskill.dto.UserSkillResponse;
import com.skillteam.userskill.dto.UserSkillUpdateRequest;
import com.skillteam.userskill.entity.Skill;
import com.skillteam.userskill.entity.UserProfile;
import com.skillteam.userskill.entity.UserSkill;
import com.skillteam.userskill.exception.ProfileNotFoundException;
import com.skillteam.userskill.exception.SkillNotFoundException;
import com.skillteam.userskill.exception.UserSkillAlreadyExistsException;
import com.skillteam.userskill.exception.UserSkillNotFoundException;
import com.skillteam.userskill.repository.SkillRepository;
import com.skillteam.userskill.repository.UserProfileRepository;
import com.skillteam.userskill.repository.UserSkillRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserSkillService {

    private static final String PROFILE_NOT_FOUND_MESSAGE = "No profile exists for this user.";
    private static final String SKILL_NOT_FOUND_MESSAGE = "No skill exists for this id.";
    private static final String ALREADY_EXISTS_MESSAGE = "This skill is already assigned to this user.";
    private static final String ASSIGNMENT_NOT_FOUND_MESSAGE = "No skill assignment exists for this user and skill.";

    private final UserSkillRepository userSkillRepository;
    private final UserProfileRepository userProfileRepository;
    private final SkillRepository skillRepository;

    public UserSkillService(UserSkillRepository userSkillRepository,
                             UserProfileRepository userProfileRepository,
                             SkillRepository skillRepository) {
        this.userSkillRepository = userSkillRepository;
        this.userProfileRepository = userProfileRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public UserSkillResponse create(Long authUserId, UserSkillRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ProfileNotFoundException(PROFILE_NOT_FOUND_MESSAGE));

        Skill skill = skillRepository.findByIdAndActiveTrue(request.skillId())
                .orElseThrow(() -> new SkillNotFoundException(SKILL_NOT_FOUND_MESSAGE));

        if (userSkillRepository.existsByUserProfileIdAndSkillId(profile.getId(), skill.getId())) {
            throw new UserSkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        UserSkill assignment = new UserSkill(profile.getId(), skill.getId(), request.proficiencyLevel());

        UserSkill saved;
        try {
            saved = userSkillRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException ex) {
            throw new UserSkillAlreadyExistsException(ALREADY_EXISTS_MESSAGE);
        }

        return toResponse(saved, skill);
    }

    @Transactional(readOnly = true)
    public List<UserSkillResponse> list(Long authUserId) {
        Optional<UserProfile> profile = userProfileRepository.findByAuthUserId(authUserId);
        if (profile.isEmpty()) {
            return List.of();
        }

        List<UserSkill> assignments = userSkillRepository.findByUserProfileId(profile.get().getId());
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> skillIds = assignments.stream().map(UserSkill::getSkillId).toList();
        Map<Long, Skill> skillsById = skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, skill -> skill));

        return assignments.stream()
                .sorted(Comparator.comparing(assignment -> skillsById.get(assignment.getSkillId()).getNormalizedName()))
                .map(assignment -> toResponse(assignment, skillsById.get(assignment.getSkillId())))
                .toList();
    }

    @Transactional
    public UserSkillResponse update(Long authUserId, Long skillId, UserSkillUpdateRequest request) {
        UserSkill assignment = findAssignmentOrThrow(authUserId, skillId);

        Skill skill = skillRepository.findByIdAndActiveTrue(skillId)
                .orElseThrow(() -> new SkillNotFoundException(SKILL_NOT_FOUND_MESSAGE));

        assignment.setProficiencyLevel(request.proficiencyLevel());
        UserSkill saved = userSkillRepository.saveAndFlush(assignment);

        return toResponse(saved, skill);
    }

    @Transactional
    public void delete(Long authUserId, Long skillId) {
        UserSkill assignment = findAssignmentOrThrow(authUserId, skillId);
        userSkillRepository.delete(assignment);
    }

    private UserSkill findAssignmentOrThrow(Long authUserId, Long skillId) {
        Optional<UserProfile> profile = userProfileRepository.findByAuthUserId(authUserId);
        if (profile.isEmpty()) {
            throw new UserSkillNotFoundException(ASSIGNMENT_NOT_FOUND_MESSAGE);
        }

        return userSkillRepository.findByUserProfileIdAndSkillId(profile.get().getId(), skillId)
                .orElseThrow(() -> new UserSkillNotFoundException(ASSIGNMENT_NOT_FOUND_MESSAGE));
    }

    private UserSkillResponse toResponse(UserSkill assignment, Skill skill) {
        return new UserSkillResponse(
                assignment.getId(),
                skill.getId(),
                skill.getName(),
                assignment.getProficiencyLevel(),
                skill.isActive(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
