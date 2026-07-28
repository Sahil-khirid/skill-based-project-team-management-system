package com.skillteam.taskprogress.client;

import java.time.Instant;

/**
 * Mirrors the response shape of the Project &amp; Team Service's member listing endpoint
 * ({@code GET /api/v1/projects/{projectId}/members}). Kept distinct from this service's own
 * response DTOs since it describes a remote API's contract, not a local resource.
 */
public record RemoteProjectMemberResponse(
        Long id,
        Long projectId,
        Long authUserId,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
