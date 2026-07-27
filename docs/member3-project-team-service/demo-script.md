# 10-Minute Demo Script — Project & Team Service

**Presenter setup, before the audience arrives:** run Postman folders `01 - Auth Setup` and
`02 - User & Skill Service Setup` in advance so the demo itself spends its time on Member 3's
actual deliverable (Project & Team Service) rather than registration/login mechanics. Have
the Eureka dashboard (`http://localhost:8761/`) and the Postman collection both open in
separate tabs/windows before starting the clock.

Every request below is a named request in `postman/Project-Team-Service.postman_collection.json`
— click through them live rather than re-typing anything.

---

### 0:00 – 1:00 — Context (no API calls)

Say: "Project & Team Service is the newest backend service in this system. It owns
projects, project membership, and each project's required skills — and it's the service
that integrates with User & Skill Service to recommend which of a project's current members
best fit its skill requirements."

### 1:00 – 2:00 — Prove the microservices are actually running

Switch to the Eureka dashboard. Point out all five registered instances:
`EUREKA-SERVER` (itself), `API-GATEWAY`, `AUTH-SERVICE`, `USER-SKILL-SERVICE`,
`PROJECT-TEAM-SERVICE`. Say: "Everything from here on goes through the Gateway on port
8080 — no direct calls to any backend service's own port."

### 2:00 – 3:30 — Project CRUD

Run, in order:
1. `03 - Project CRUD - Happy Path → Create Project (PM)` — point out the response:
   `status: "PLANNING"`, `ownerAuthUserId` set to the PM's own id.
2. `03 - Project CRUD - Happy Path → List Projects` — show it in the list.
3. `03 - Project CRUD - Happy Path → Update Project - Set Status ACTIVE (PM)` — show
   `status` flip to `"ACTIVE"`.

Say while it runs: "Every write here requires a PROJECT_MANAGER role — that's enforced
inside this service itself, using the identity headers the Gateway attaches after
validating the JWT."

### 3:30 – 5:00 — Project Members

Run:
1. `05 - Project Members - Happy Path → Add Member 1 (PM)` — Alice, as `LEADER`.
2. `05 - Project Members - Happy Path → Add Member 2 (PM)` — Bob, as `MEMBER`.
3. `05 - Project Members - Happy Path → List Project Members` — show both.

Say: "These member ids are the exact same Auth Service user ids from login — that shared
identity is what lets the recommendation engine later ask User & Skill Service 'what does
this specific person know.'"

### 5:00 – 6:30 — Project Required Skills

Run:
1. `07 - Project Required Skills - Happy Path → Add Required Skill - Java INTERMEDIATE (PM)`
2. `07 - Project Required Skills - Happy Path → Add Required Skill - SQL INTERMEDIATE (PM)`
3. `07 - Project Required Skills - Happy Path → List Required Skills`

Say: "This is the project's skill requirement — Java and SQL, both at least at
Intermediate level. Nothing here talks to User & Skill Service yet; that only happens at
recommendation time."

### 6:30 – 8:30 — The main event: Member Recommendations

Run `09 - Member Recommendations → Get Recommendations - Happy Path (PM)`.

Walk through the response live:
- Alice ranks first: `matchPercentage: 100.0`, `matchedSkillCount: 2`, `missingSkillCount: 0`
  — she has Java at Expert and SQL at Intermediate, both meeting the bar.
- Bob ranks second: `matchPercentage: 0.0`, `missingSkillCount: 2` — he has no Java at all,
  and his SQL is only Beginner, below the required Intermediate level.

Say: "This response is computed live, on every call — nothing is cached or pre-stored.
Project & Team Service just called User & Skill Service over the network, using
Eureka-based service discovery, forwarding my own identity as the caller. If I changed
Bob's SQL skill right now in User & Skill Service, the very next call here would reflect
it immediately."

Say: "The ranking itself is deterministic: highest match percentage first, and if two
members ever tie exactly, more matched skills wins, and if that's still tied, lower user id
wins — so the order is never ambiguous or dependent on database row order."

### 8:30 – 9:30 — Error handling

Run:
1. `09 - Member Recommendations → Get Recommendations - As USER (403)` — switch to a
   member's own token and show it's rejected; only a PROJECT_MANAGER may view
   recommendations.
2. `09 - Member Recommendations → Get Recommendations - Project Not Found (404)` — show a
   clean, consistent error shape for a bad project id.

*(Optional, only if time and a second terminal permit): stop the User & Skill Service
process, then run `Get Recommendations - User & Skill Service Down (MANUAL, 503)` to show
the fail-fast behavior — the request fails immediately with `503` rather than hanging or
returning incomplete data.*

### 9:30 – 10:00 — Wrap-up

Say: "That's Project CRUD, member management, required skills, and the recommendation
engine — all built on top of the existing identity model, all routed through the same
Gateway, and the recommendation piece is the first place in this system where two backend
services actually talk to each other live." Open for questions.
