package com.skillteam.projectteam.dto;

import java.util.List;

public record MemberRecommendationResponse(
        Long authUserId,
        int matchedSkillCount,
        int requiredSkillCount,
        int missingSkillCount,
        double matchPercentage,
        List<SkillMatchDetail> matchedSkills,
        List<Long> missingSkillIds
) {
}
