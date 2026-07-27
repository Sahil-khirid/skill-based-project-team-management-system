# Project & Team Service — API Reference

Service name: `PROJECT-TEAM-SERVICE` · Port: `8083` · Gateway route: `/api/v1/projects/**`
→ `lb://PROJECT-TEAM-SERVICE` (no path rewriting — the path a client sends to the Gateway
on port `8080` is identical to the path this service receives).

All endpoints require `X-Auth-User-Id` / `X-Auth-User-Role` — supplied by the Gateway from
a validated JWT, never set directly by a client. `PROJECT_MANAGER`-only endpoints are
enforced inside this service, not at the Gateway. Every error response uses the same shape:

```json
{
  "timestamp": "2026-07-27T10:16:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No project exists for this id.",
  "path": "/api/v1/projects/999999",
  "fieldErrors": []
}
```

Enums used throughout: `ProficiencyLevel` = `BEGINNER | INTERMEDIATE | ADVANCED | EXPERT`
(ordered low to high). `ProjectMemberRole` = `MEMBER | LEADER`. `ProjectStatus` =
`PLANNING | ACTIVE | ON_HOLD | COMPLETED | CANCELLED`.

---

## Projects

### `POST /api/v1/projects` — Create project
**Role:** PROJECT_MANAGER
```json
// Request
{ "name": "Apollo Platform Rebuild", "description": "Modernize the core platform services." }
```
```json
// 201 Created
{
  "id": 1, "name": "Apollo Platform Rebuild",
  "description": "Modernize the core platform services.",
  "status": "PLANNING", "ownerAuthUserId": 7,
  "createdAt": "2026-07-27T10:00:00Z", "updatedAt": "2026-07-27T10:00:00Z"
}
```
Errors: `400` blank/too-long name; `409` duplicate name (case/whitespace-insensitive);
`403` caller is not PROJECT_MANAGER.

### `GET /api/v1/projects` — List all projects
**Role:** any authenticated. Returns `200` with a `ProjectResponse[]`, ordered by
normalized name. Empty array if none exist.

### `GET /api/v1/projects/{id}` — Get one project
**Role:** any authenticated. `200` with the project, or `404` if the id doesn't exist.

### `PUT /api/v1/projects/{id}` — Update project
**Role:** PROJECT_MANAGER
```json
// Request — status is required on every update, not just name/description
{ "name": "Apollo Platform Rebuild", "description": "Modernize the core platform services.", "status": "ACTIVE" }
```
`200` with the updated project. Errors: `404` project not found; `409` name conflicts with
a *different* project; `400` invalid `status` literal or blank name; `403` non-manager.

### `DELETE /api/v1/projects/{id}` — Delete project
**Role:** PROJECT_MANAGER. `204 No Content`. `404` if already deleted/never existed.
Deleting a project does not cascade-verify members/required-skills in the response — they
are removed with it at the database level.

---

## Project Members

### `POST /api/v1/projects/{projectId}/members` — Add a member
**Role:** PROJECT_MANAGER
```json
// Request
{ "authUserId": 12, "role": "MEMBER" }
```
```json
// 201 Created
{
  "id": 3, "projectId": 1, "authUserId": 12, "role": "MEMBER",
  "createdAt": "2026-07-27T10:05:00Z", "updatedAt": "2026-07-27T10:05:00Z"
}
```
`authUserId` is the Auth Service user id — **not validated for existence** against Auth
Service at write time (an id for a nonexistent account is accepted; it will simply score
0% on every future recommendation call). Errors: `404` project not found; `409` user
already a member of this project; `403` non-manager.

### `GET /api/v1/projects/{projectId}/members` — List members
**Role:** any authenticated. `200` with a `ProjectMemberResponse[]`, ordered by insertion.
`404` if the project doesn't exist.

### `DELETE /api/v1/projects/{projectId}/members/{authUserId}` — Remove a member
**Role:** PROJECT_MANAGER. `204 No Content`. `404` if that user isn't currently a member
(also returned if the project itself doesn't exist).

---

## Project Required Skills

### `POST /api/v1/projects/{projectId}/required-skills` — Add a required skill
**Role:** PROJECT_MANAGER
```json
// Request
{ "skillId": 5, "proficiencyLevel": "INTERMEDIATE" }
```
```json
// 201 Created
{
  "id": 2, "projectId": 1, "skillId": 5, "proficiencyLevel": "INTERMEDIATE",
  "createdAt": "2026-07-27T10:06:00Z", "updatedAt": "2026-07-27T10:06:00Z"
}
```
`skillId` is a User & Skill Service id — **not validated for existence** at write time,
same tradeoff as `authUserId` above (an id for a nonexistent skill is accepted; it will
simply appear in every member's `missingSkillIds` forever). Errors: `404` project not
found; `409` skill already required by this project (the same skill may be required by
multiple *different* projects); `400` invalid `proficiencyLevel` literal; `403` non-manager.

### `GET /api/v1/projects/{projectId}/required-skills` — List required skills
**Role:** any authenticated. `200` with a `ProjectRequiredSkillResponse[]`, ordered by
insertion. Note: **no skill name** is included — only `skillId` — since this service does
not store skill names locally.

### `DELETE /api/v1/projects/{projectId}/required-skills/{skillId}` — Remove a required skill
**Role:** PROJECT_MANAGER. `204 No Content`. `404` if that skill isn't currently required
by this project.

---

## Member Recommendations

### `GET /api/v1/projects/{projectId}/member-recommendations` — Rank current members against required skills
**Role:** PROJECT_MANAGER. Computed live on every call — nothing is cached or persisted.

```json
// 200 OK
{
  "projectId": 1,
  "recommendations": [
    {
      "authUserId": 12,
      "matchedSkillCount": 2,
      "requiredSkillCount": 2,
      "missingSkillCount": 0,
      "matchPercentage": 100.0,
      "matchedSkills": [
        { "skillId": 5, "skillName": "Java", "requiredLevel": "INTERMEDIATE", "memberLevel": "EXPERT" },
        { "skillId": 6, "skillName": "SQL", "requiredLevel": "INTERMEDIATE", "memberLevel": "INTERMEDIATE" }
      ],
      "missingSkillIds": []
    },
    {
      "authUserId": 13,
      "matchedSkillCount": 0,
      "requiredSkillCount": 2,
      "missingSkillCount": 2,
      "matchPercentage": 0.0,
      "matchedSkills": [],
      "missingSkillIds": [5, 6]
    }
  ]
}
```

**Matching rule:** a required skill counts as matched only if the member holds it, it is
**active** in User & Skill Service, and `memberLevel >= requiredLevel` using the ordering
`BEGINNER < INTERMEDIATE < ADVANCED < EXPERT`. Anything else — the member doesn't have the
skill, has it but deactivated, or has it below the required level — counts identically as
missing (no partial credit).

**Ranking (deterministic):** highest `matchPercentage` first; ties broken by higher
`matchedSkillCount`; remaining ties broken by lower `authUserId`.

**Edge cases:** a project with no members returns `recommendations: []`. A project with no
required skills gives every member `matchPercentage: 100.0`, `requiredSkillCount: 0`. A
member with no User & Skill Service profile at all still appears, scoring `0.0` — this is
not an error condition.

**Errors:** `404` project not found; `403` caller is not PROJECT_MANAGER; `503` if User &
Skill Service is unreachable while computing any member's score — the entire request fails
rather than returning a partial/degraded result.

---

## Cross-service identity note

`authUserId` everywhere in this service (project owner, member, and the caller identity
used to reach this API) is the exact same numeric id issued by Auth Service at
registration/login (`sub` claim → `X-Auth-User-Id` header). The same id is also what User
& Skill Service uses to key profiles and skill assignments. There is no separate
"Project & Team Service user id" — it's one shared identity space across all three
services.
