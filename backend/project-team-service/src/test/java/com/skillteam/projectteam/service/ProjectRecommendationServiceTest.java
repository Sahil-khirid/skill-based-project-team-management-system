package com.skillteam.projectteam.service;

import com.skillteam.projectteam.client.RemoteUserSkillResponse;
import com.skillteam.projectteam.client.UserSkillServiceClient;
import com.skillteam.projectteam.dto.MemberRecommendationResponse;
import com.skillteam.projectteam.dto.ProjectRecommendationResponse;
import com.skillteam.projectteam.entity.ProficiencyLevel;
import com.skillteam.projectteam.entity.ProjectMember;
import com.skillteam.projectteam.entity.ProjectMemberRole;
import com.skillteam.projectteam.entity.ProjectRequiredSkill;
import com.skillteam.projectteam.exception.ProjectNotFoundException;
import com.skillteam.projectteam.exception.UserSkillServiceUnavailableException;
import com.skillteam.projectteam.repository.ProjectMemberRepository;
import com.skillteam.projectteam.repository.ProjectRepository;
import com.skillteam.projectteam.repository.ProjectRequiredSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectRecommendationServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long CALLER_USER_ID = 1L;
    private static final String CALLER_ROLE = "PROJECT_MANAGER";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectRequiredSkillRepository projectRequiredSkillRepository;

    @Mock
    private UserSkillServiceClient userSkillServiceClient;

    @InjectMocks
    private ProjectRecommendationService projectRecommendationService;

    private ProjectMember member(Long authUserId) {
        return new ProjectMember(PROJECT_ID, authUserId, ProjectMemberRole.MEMBER);
    }

    private ProjectRequiredSkill requiredSkill(Long skillId, ProficiencyLevel level) {
        return new ProjectRequiredSkill(PROJECT_ID, skillId, level);
    }

    private RemoteUserSkillResponse remoteSkill(Long skillId, String skillName, ProficiencyLevel level, boolean active) {
        return new RemoteUserSkillResponse(skillId, skillId, skillName, level, active, Instant.now(), Instant.now());
    }

    // --- project existence ---

    @Test
    void recommendForNonexistentProjectIsTranslatedToProjectNotFoundException() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> projectRecommendationService.recommend(999L, CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("No project exists for this id.");
    }

    // --- empty members ---

    @Test
    void recommendWithNoMembersReturnsEmptyListAndNeverCallsUserSkillServiceClient() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.BEGINNER)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of());

        ProjectRecommendationResponse response =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.projectId()).isEqualTo(PROJECT_ID);
        assertThat(response.recommendations()).isEmpty();
        verifyNoInteractions(userSkillServiceClient);
    }

    // --- zero required skills ---

    @Test
    void recommendWithNoRequiredSkillsGivesEveryMemberFullMatchPercentage() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of());
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE)).thenReturn(List.of());

        ProjectRecommendationResponse response =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE);

        MemberRecommendationResponse recommendation = response.recommendations().get(0);
        assertThat(recommendation.matchedSkillCount()).isZero();
        assertThat(recommendation.requiredSkillCount()).isZero();
        assertThat(recommendation.missingSkillCount()).isZero();
        assertThat(recommendation.matchPercentage()).isEqualTo(100.0);
        assertThat(recommendation.matchedSkills()).isEmpty();
        assertThat(recommendation.missingSkillIds()).isEmpty();
    }

    // --- member with no skills at all ---

    @Test
    void memberWithNoSkillsHasAllRequiredSkillsMissing() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.BEGINNER),
                        requiredSkill(101L, ProficiencyLevel.ADVANCED)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE)).thenReturn(List.of());

        MemberRecommendationResponse recommendation =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE)
                        .recommendations().get(0);

        assertThat(recommendation.matchedSkillCount()).isZero();
        assertThat(recommendation.missingSkillCount()).isEqualTo(2);
        assertThat(recommendation.missingSkillIds()).containsExactlyInAnyOrder(100L, 101L);
        assertThat(recommendation.matchPercentage()).isEqualTo(0.0);
    }

    // --- inactive skill treated as missing ---

    @Test
    void inactiveMemberSkillAtOrAboveRequiredLevelIsTreatedAsMissing() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.BEGINNER)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE))
                .thenReturn(List.of(remoteSkill(100L, "Java", ProficiencyLevel.EXPERT, false)));

        MemberRecommendationResponse recommendation =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE)
                        .recommendations().get(0);

        assertThat(recommendation.matchedSkillCount()).isZero();
        assertThat(recommendation.missingSkillIds()).containsExactly(100L);
    }

    // --- proficiency below required level treated as missing ---

    @Test
    void memberSkillBelowRequiredProficiencyIsTreatedAsMissing() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.ADVANCED)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE))
                .thenReturn(List.of(remoteSkill(100L, "Java", ProficiencyLevel.INTERMEDIATE, true)));

        MemberRecommendationResponse recommendation =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE)
                        .recommendations().get(0);

        assertThat(recommendation.matchedSkillCount()).isZero();
        assertThat(recommendation.missingSkillIds()).containsExactly(100L);
    }

    // --- proficiency at or above required level is matched ---

    @Test
    void memberSkillAtOrAboveRequiredProficiencyIsMatched() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.INTERMEDIATE)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE))
                .thenReturn(List.of(remoteSkill(100L, "Java", ProficiencyLevel.EXPERT, true)));

        MemberRecommendationResponse recommendation =
                projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE)
                        .recommendations().get(0);

        assertThat(recommendation.matchedSkillCount()).isEqualTo(1);
        assertThat(recommendation.missingSkillIds()).isEmpty();
        assertThat(recommendation.matchPercentage()).isEqualTo(100.0);
        assertThat(recommendation.matchedSkills().get(0).skillId()).isEqualTo(100L);
        assertThat(recommendation.matchedSkills().get(0).skillName()).isEqualTo("Java");
        assertThat(recommendation.matchedSkills().get(0).requiredLevel()).isEqualTo(ProficiencyLevel.INTERMEDIATE);
        assertThat(recommendation.matchedSkills().get(0).memberLevel()).isEqualTo(ProficiencyLevel.EXPERT);
    }

    // --- identity forwarding ---

    @Test
    void recommendForwardsCallerIdentityToUserSkillServiceClient() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of());
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(eq(10L), eq(42L), eq("PROJECT_MANAGER"))).thenReturn(List.of());

        projectRecommendationService.recommend(PROJECT_ID, 42L, "PROJECT_MANAGER");

        verify(userSkillServiceClient).fetchSkills(10L, 42L, "PROJECT_MANAGER");
    }

    // --- downstream failure propagates (fail-fast) ---

    @Test
    void downstreamFailureForAnyMemberPropagatesAndAbortsTheWholeRequest() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
        when(projectRequiredSkillRepository.findByProjectIdOrderByIdAsc(PROJECT_ID))
                .thenReturn(List.of(requiredSkill(100L, ProficiencyLevel.BEGINNER)));
        when(projectMemberRepository.findByProjectIdOrderByIdAsc(PROJECT_ID)).thenReturn(List.of(member(10L)));
        when(userSkillServiceClient.fetchSkills(10L, CALLER_USER_ID, CALLER_ROLE))
                .thenThrow(new UserSkillServiceUnavailableException("downstream unavailable", new RuntimeException()));

        assertThatThrownBy(() -> projectRecommendationService.recommend(PROJECT_ID, CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(UserSkillServiceUnavailableException.class);
    }

    // --- deterministic ranking ---

    private MemberRecommendationResponse recommendation(Long authUserId, int matchedSkillCount, double matchPercentage) {
        return new MemberRecommendationResponse(authUserId, matchedSkillCount, 4, 4 - matchedSkillCount,
                matchPercentage, List.of(), List.of());
    }

    @Test
    void rankingOrdersByMatchPercentageDescendingFirst() {
        MemberRecommendationResponse low = recommendation(1L, 1, 25.0);
        MemberRecommendationResponse high = recommendation(2L, 3, 75.0);

        List<MemberRecommendationResponse> ranked =
                List.of(low, high).stream().sorted(ProjectRecommendationService.RANKING_ORDER).toList();

        assertThat(ranked).containsExactly(high, low);
    }

    @Test
    void rankingBreaksMatchPercentageTiesByMatchedSkillCountDescending() {
        MemberRecommendationResponse fewerMatches = recommendation(1L, 1, 50.0);
        MemberRecommendationResponse moreMatches = recommendation(2L, 2, 50.0);

        List<MemberRecommendationResponse> ranked =
                List.of(fewerMatches, moreMatches).stream().sorted(ProjectRecommendationService.RANKING_ORDER).toList();

        assertThat(ranked).containsExactly(moreMatches, fewerMatches);
    }

    @Test
    void rankingBreaksRemainingTiesByAuthUserIdAscending() {
        MemberRecommendationResponse higherId = recommendation(9L, 2, 50.0);
        MemberRecommendationResponse lowerId = recommendation(3L, 2, 50.0);

        List<MemberRecommendationResponse> ranked =
                List.of(higherId, lowerId).stream().sorted(ProjectRecommendationService.RANKING_ORDER).toList();

        assertThat(ranked).containsExactly(lowerId, higherId);
    }
}
