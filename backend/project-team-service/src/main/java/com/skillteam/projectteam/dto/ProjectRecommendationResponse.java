package com.skillteam.projectteam.dto;

import java.util.List;

public record ProjectRecommendationResponse(
        Long projectId,
        List<MemberRecommendationResponse> recommendations
) {
}
