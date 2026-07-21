# ADR-001: Technology Version Baseline

## Status

Accepted

## Date

2026-07-21

## Context

The Skill-Based Project Team Management System is a multi-module, microservices-based
project shared by four project members. Multiple backend services (Eureka Server, API
Gateway, Auth Service, User & Skill Service, Project & Team Service, Task & Progress
Service) and a frontend application are planned. Without an agreed, written version
baseline, each member could independently choose different Java, Spring Boot, Spring
Cloud, or build-tool versions, leading to dependency conflicts, inconsistent builds, and
integration failures between services (in particular between Eureka clients and the
Eureka server).

This ADR records the technology version baseline approved for Milestone 1 (Repository
Foundation and Eureka Server) so that all current and future modules in this repository
build against a single, consistent set of versions.

## Decision

Adopt the following technology version baseline for the entire repository. The root
Maven POM centralizes these versions via dependency management so that all current and
future modules inherit them consistently.

## Approved Versions

- Java: 21 (LTS)
- Spring Boot: 3.5.16
- Spring Cloud: 2025.0.3
- Build tool: Maven 3.9.16, invoked via the committed Maven Wrapper
- Maven Wrapper Plugin: 3.3.4
- MySQL: 8.4 (reserved for future business services; not used by Eureka Server)
- Frontend: React with Vite (reserved for the future frontend application; not
  implemented in this milestone)

## Rationale

- Java 21 is an LTS release and provides a stable baseline for the project, and is
  compatible with the chosen Spring Boot and Spring Cloud release trains.
- Spring Boot 3.5.16 and Spring Cloud 2025.0.3 are a matched, mutually compatible
  release pair, imported as BOMs from the root POM so child modules do not need to
  pin individual Spring dependency versions themselves.
- Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed to the repository so that all four
  project members and any CI environment build with the exact same Maven version,
  removing "works on my machine" version drift.
- MySQL 8.4 and React with Vite are recorded here as forward-looking decisions for
  services and the frontend planned in later milestones, so that future contributors
  do not need to re-litigate the choice when those modules are created. They are not
  used or configured anywhere in this milestone.

## Consequences

- All current and future backend modules must target Java 21 and inherit Spring Boot
  3.5.16 / Spring Cloud 2025.0.3 dependency management from the root POM rather than
  declaring their own versions.
- Contributors must use the committed Maven Wrapper (`mvnw` / `mvnw.cmd`) rather than a
  locally installed Maven, to guarantee a consistent build across machines.
- Introducing MySQL or a frontend build tool other than Vite in a later milestone would
  contradict this ADR and would require a new or superseding ADR.
- Any change to the versions recorded in this ADR affects every module in the
  repository and therefore requires explicit approval before implementation (see Rule
  below).

## Alternatives Considered

- **Gradle instead of Maven**: rejected for this milestone; Maven with a committed
  wrapper was chosen for its wide familiarity across the team and straightforward
  multi-module aggregator/BOM support.
- **Per-module, independently chosen Spring Boot/Spring Cloud versions**: rejected, as
  it would risk incompatible dependency combinations between services and the shared
  Eureka registry.
- **Locally installed Maven without a wrapper**: rejected, as it does not guarantee
  build reproducibility across the four project members' machines.

## Rule: Version Changes Require Approval

Any change to the Java, Spring Boot, Spring Cloud, Maven, Maven Wrapper Plugin, MySQL,
or frontend framework versions recorded in this ADR must be explicitly approved (e.g.
by a new ADR that supersedes this one) before being implemented in the repository. Do
not upgrade or downgrade these versions unilaterally in a feature branch.
