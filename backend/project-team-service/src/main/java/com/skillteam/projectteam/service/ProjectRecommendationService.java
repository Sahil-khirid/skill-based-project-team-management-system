package com.skillteam.projectteam.service;

import com.skillteam.projectteam.client.RemoteUserSkillResponse;
import com.skillteam.projectteam.client.UserSkillServiceClient;
import com.skillteam.projectteam.dto.MemberRecommendationResponse;
import com.skillteam.projectteam.dto.ProjectRecommendationResponse;
import com.skillteam.projectteam.dto.SkillMatchDetail;
import com.skillteam.projectteam.entity.ProjectMember;
import com.skillteam.projectteam.entity.ProjectRequiredSkill;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProjectRecommendationService {

    private static final String PROJECT_NOT_FOUND_MESSAGE = "No project exists for this id.";

    /**
     * Ranks members by matchPercentage desc, then matchedSkillCount desc, then authUserId asc.
     * Package-private so the ranking rules can be unit-tested directly: matchPercentage and
     * matchedSkillCount share the same requiredSkillCount denominator within one recommend() call,
     * so the matchedSkillCount tie-break can never be exercised through the public API alone.
     */
    static final Comparator<MemberRecommendationResponse> RANKING_ORDER = Comparator
            .comparingDouble(MemberRecommendationResponse::matchPercentage).reversed()
            .thenComparing(Comparator.comparingInt(MemberRecommendationResponse::matchedSkillCount).reversed())
            .thenComparing(MemberRecommendationResponse::authUserId);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRequiredSkillRepository projectRequiredSkillRepository;
    private final UserSkillServiceClient userSkillServiceClient;

    public ProjectRecommendationService(ProjectRepository projectRepository,
                                         ProjectMemberRepository projectMemberRepository,
                                         ProjectRequiredSkillRepository projectRequiredSkillRepository,
                                         UserSkillServiceClient userSkillServiceClient) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectRequiredSkillRepository = projectRequiredSkillRepository;
        this.userSkillServiceClient = userSkillServiceClient;
    }

    public ProjectRecommendationResponse recommend(Long projectId, Long callerUserId, String callerRole) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(PROJECT_NOT_FOUND_MESSAGE);
        }

        List<ProjectRequiredSkill> requiredSkills = projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(projectId);
        List<ProjectMember> members = projectMemberRepository.findByProjectIdOrderByIdAsc(projectId);

        List<MemberRecommendationResponse> recommendations = members.stream()
                .map(member -> score(member, requiredSkills, callerUserId, callerRole))
                .sorted(RANKING_ORDER)
                .toList();

        return new ProjectRecommendationResponse(projectId, recommendations);
    }

    private MemberRecommendationResponse score(ProjectMember member, List<ProjectRequiredSkill> requiredSkills,
                                                Long callerUserId, String callerRole) {
        List<RemoteUserSkillResponse> memberSkills =
                userSkillServiceClient.fetchSkills(member.getAuthUserId(), callerUserId, callerRole);

        Map<Long, RemoteUserSkillResponse> memberSkillsBySkillId = memberSkills.stream()
                .collect(Collectors.toMap(RemoteUserSkillResponse::skillId, Function.identity(), (first, second) -> first));

        List<SkillMatchDetail> matchedSkills = new ArrayList<>();
        List<Long> missingSkillIds = new ArrayList<>();

        for (ProjectRequiredSkill requiredSkill : requiredSkills) {
            RemoteUserSkillResponse memberSkill = memberSkillsBySkillId.get(requiredSkill.getSkillId());

            // A required skill is matched only when the member holds it, it is active, and their
            // proficiency is at least the required level. An inactive skill or a lower proficiency
            // both count as missing for scoring purposes, not a partial match.
            boolean matched = memberSkill != null
                    && memberSkill.skillActive()
                    && memberSkill.proficiencyLevel().ordinal() >= requiredSkill.getProficiencyLevel().ordinal();

            if (matched) {
                matchedSkills.add(new SkillMatchDetail(
                        requiredSkill.getSkillId(),
                        memberSkill.skillName(),
                        requiredSkill.getProficiencyLevel(),
                        memberSkill.proficiencyLevel()));
            } else {
                missingSkillIds.add(requiredSkill.getSkillId());
            }
        }

        int requiredSkillCount = requiredSkills.size();
        int matchedSkillCount = matchedSkills.size();
        int missingSkillCount = missingSkillIds.size();
        double rawMatchPercentage = requiredSkillCount == 0 ? 100.0 : (matchedSkillCount * 100.0 / requiredSkillCount);
        double matchPercentage = Math.round(rawMatchPercentage * 100.0) / 100.0;

        return new MemberRecommendationResponse(
                member.getAuthUserId(),
                matchedSkillCount,
                requiredSkillCount,
                missingSkillCount,
                matchPercentage,
                matchedSkills,
                missingSkillIds
        );
    }
}
