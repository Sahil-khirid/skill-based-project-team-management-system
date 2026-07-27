package com.skillteam.projectteam.client;

import com.skillteam.projectteam.entity.ProficiencyLevel;

import java.time.Instant;

/**
 * Mirrors the response shape of the User &amp; Skill Service's internal lookup endpoint
 * ({@code GET /api/v1/users/{authUserId}/skills}). Kept distinct from this service's own
 * response DTOs since it describes a remote API's contract, not a local resource.
 */
public record RemoteUserSkillResponse(
        Long id,
        Long skillId,
        String skillName,
        ProficiencyLevel proficiencyLevel,
        boolean skillActive,
        Instant createdAt,
        Instant updatedAt
) {
}
