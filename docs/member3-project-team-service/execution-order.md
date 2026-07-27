# Project & Team Service — End-to-End Execution Order

Companion to the Postman collection `postman/Project-Team-Service.postman_collection.json`
and environment `postman/Project-Team-Service.postman_environment.json`. Import both into
Postman, select the environment, and run the folders in the order below — either by hand or
via the Collection Runner set to run the whole collection top-to-bottom.

## 0. Service startup order (must be running before any request)

```
1. MySQL — with skillteam_auth, skillteam_user, and skillteam_project databases created
2. Eureka Server          (port 8761)
3. Auth Service           (port 8081)
4. User & Skill Service   (port 8082)
5. Project & Team Service (port 8083)
6. API Gateway            (port 8080)
```
Steps 4 before 5 matters: the Member Recommendation endpoint calls User & Skill Service
synchronously. Wait for `USER-SKILL-SERVICE` to appear in the Eureka dashboard
(`http://localhost:8761/`) before calling any Project & Team Service endpoint that touches
recommendations.

## 1. One manual step this collection cannot automate

There is no API endpoint anywhere in this system that promotes a user to `PROJECT_MANAGER`
— every account is created as `USER`. Between running **01 → Register PM** and
**01 → Login PM**, run this against the `skillteam_auth` database:

```sql
UPDATE auth_users SET role = 'PROJECT_MANAGER' WHERE email = 'pm.member3@example.com';
```

If this step is skipped, `Login PM` will still succeed, but its role assertion test will
fail and every PROJECT_MANAGER-only request afterward will return `403`.

## 2. Folder execution order

Run in this exact order. Each folder's requests are already ordered internally and use
Postman test scripts to capture ids/tokens into environment variables automatically — no
manual copy-pasting is required.

| Order | Folder | Purpose | Produces |
|---|---|---|---|
| 1 | `01 - Auth Setup (Prerequisite)` | Register + log in the PM and two members | `pmToken`, `pmId`, `member1Token`, `member1Id`, `member2Token`, `member2Id` |
| 2 | `02 - User & Skill Service Setup (Prerequisite)` | Create the skill catalog, member profiles, and member skill assignments | `javaSkillId`, `sqlSkillId` |
| 3 | `03 - Project CRUD - Happy Path` | Create the demo project, list/get/update it | `projectId` |
| 4 | `04 - Project CRUD - Negative Cases` | Duplicate name, wrong role, blank name, not-found | — |
| 5 | `05 - Project Members - Happy Path` | Add both members to the project | — |
| 6 | `06 - Project Members - Negative Cases` | Duplicate member, wrong role, project not found | — |
| 7 | `07 - Project Required Skills - Happy Path` | Require Java and SQL at INTERMEDIATE | — |
| 8 | `08 - Project Required Skills - Negative Cases` | Duplicate skill, invalid enum literal, wrong role | — |
| 9 | `09 - Member Recommendations` | The full demo: ranked recommendations, wrong-role, not-found, and the manual downstream-failure case | — |
| 10 | `10 - Cleanup and Destructive Tests (Run Last)` | Remove member, remove required skill, delete project, then repeat each to confirm 404 | — |

Folder 10 is deliberately last: it deletes everything folders 3–9 depend on, so running it
earlier would break every subsequent folder.

## 3. What each folder proves

- **Folders 1–2** prove the cross-service identity model: the same numeric id issued by
  Auth Service at registration is the id Project & Team Service stores as a project member,
  and the id User & Skill Service keys profiles/skills against.
- **Folders 3, 5, 7** prove core CRUD correctness for Member 3's three domain resources.
- **Folders 4, 6, 8** prove authorization (`PROJECT_MANAGER`-only writes), validation, and
  not-found handling are correctly enforced on every write path.
- **Folder 9** is the integration centerpiece: it proves the live call from Project & Team
  Service to User & Skill Service actually works end-to-end (not just against a mocked test),
  proves the ranking is deterministic, and proves the fail-fast `503` behavior when the
  downstream service is unavailable.
- **Folder 10** proves delete/remove paths and that repeated removal correctly 404s instead
  of silently succeeding twice.

## 4. Re-running the whole collection

Because folder 10 fully deletes the project, its members, and its required skills, the
collection is safe to re-run from folder 3 onward on a second pass (folders 1–2 will fail on
duplicate-email registration if re-run — re-run only folders 3–10, or use fresh emails in
the environment for a second Auth Setup pass).
