# Demo Data — Project & Team Service

Exact data set the Postman collection creates, and the reasoning behind the specific
numbers chosen so the recommendation demo produces a clear, easy-to-narrate contrast.

## Accounts (Auth Service)

| Role | Email | Password | Notes |
|---|---|---|---|
| PROJECT_MANAGER | `pm.member3@example.com` | `Password@123` | Requires the manual DB promotion — see `execution-order.md` §1 |
| USER (Member 1 — "Alice Johnson") | `alice.member3@example.com` | `Password@123` | The strong match |
| USER (Member 2 — "Bob Martinez") | `bob.member3@example.com` | `Password@123` | The weak match |

## Skill catalog (User & Skill Service)

| Skill | Description |
|---|---|
| Java | Core Java and the JVM ecosystem. |
| SQL | Relational database querying and design. |

## Member profiles and skills (User & Skill Service)

| Member | Profile | Skills |
|---|---|---|
| Alice (Member 1) | Display name "Alice Johnson", headline "Backend Engineer" | Java @ `EXPERT`, SQL @ `INTERMEDIATE` |
| Bob (Member 2) | Display name "Bob Martinez", headline "Junior Developer" | SQL @ `BEGINNER` only — **no Java assignment at all** |

## Project (Project & Team Service)

| Field | Value |
|---|---|
| Name | Apollo Platform Rebuild |
| Description | Modernize the core platform services. |
| Status | `PLANNING` → updated to `ACTIVE` during the demo |
| Required skills | Java @ `INTERMEDIATE`, SQL @ `INTERMEDIATE` |
| Members | Alice (`LEADER`), Bob (`MEMBER`) |

## Why these exact numbers were chosen

The required bar for both skills is `INTERMEDIATE`. `ProficiencyLevel` ordering is
`BEGINNER < INTERMEDIATE < ADVANCED < EXPERT`, and a required skill only counts as matched
when the member's level is **at or above** the required level:

- **Alice**: Java `EXPERT` ≥ `INTERMEDIATE` → matched. SQL `INTERMEDIATE` ≥ `INTERMEDIATE` →
  matched. Result: **2 of 2 matched, 100% match**.
- **Bob**: has no Java assignment at all → automatically missing. SQL `BEGINNER` <
  `INTERMEDIATE` → missing even though he *has* the skill, just not at the required level.
  Result: **0 of 2 matched, 0% match**.

This gives the recommendation endpoint a clean, unambiguous 100% vs. 0% contrast — ideal
for a live demo, since the audience doesn't need to do any mental math to see the ranking is
correct, and it simultaneously demonstrates two different "missing" reasons (skill absent
vs. skill present but underqualified) in a single call.

## Resetting between demo runs

The Postman collection's folder `10 - Cleanup and Destructive Tests` removes both members,
both required skills, and deletes the project itself, but does **not** delete the Auth
Service accounts or the User & Skill Service skill catalog/profiles (those have no delete
endpoints exposed for accounts, and deleting the catalog isn't necessary between runs).
To run the full demo again from scratch: re-run folders `03` through `10` only — do not
re-run folder `01`/`02` unless you first clear the corresponding rows from `skillteam_auth`
and `skillteam_user`, since duplicate email/skill-name registration will otherwise fail
with `409 Conflict`.
