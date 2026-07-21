# Skill-Based Project Team Management System

A microservices-based project management system for skill-based team formation, task assignment, and progress tracking.

## Status

This project is under active development across four project members. The sections
below draw a clear line between what is implemented today and what is planned.

### Implemented (Milestone 1 — Repository Foundation and Eureka Server)

- Root Maven aggregator / dependency-management POM (Java 21, Spring Boot 3.5.16,
  Spring Cloud 2025.0.3 BOMs).
- Committed Maven Wrapper (`mvnw`, `mvnw.cmd`).
- Standalone Eureka Server (`backend/eureka-server`), application name
  `EUREKA-SERVER`, listening on port `8761`.
- Spring Boot Actuator `health` endpoint on the Eureka Server.
- ADR documenting the approved technology version baseline
  (`docs/architecture/adr/ADR-001-technology-version-baseline.md`).

### Planned (not yet implemented)

- API Gateway
- Auth Service and JWT-based security
- User & Skill Service
- Project & Team Service
- Task & Progress Service
- React (Vite) frontend
- MySQL-backed persistence, Spring Data JPA, Flyway migrations

Advanced deployment infrastructure (Docker, CI/CD, Kubernetes) is excluded from
Version 1.

Do not assume any service other than Eureka Server is functional yet.

## Requirements

- Java 21
- Spring Boot 3.5.16 (managed by the root POM — no local install needed)
- Spring Cloud 2025.0.3 (managed by the root POM — no local install needed)
- No local Maven install is required; use the committed Maven Wrapper (`mvnw` /
  `mvnw.cmd`).

### Verify your Java version

```
java -version
```

Confirm the output reports major version `21`.

## Building the project

From the repository root:

**Windows**

```
mvnw.cmd clean verify
```

**Linux / macOS**

```
./mvnw clean verify
```

## Running the Eureka Server

**Windows**

```
mvnw.cmd -pl backend/eureka-server spring-boot:run
```

**Linux / macOS**

```
./mvnw -pl backend/eureka-server spring-boot:run
```

Once running:

- Eureka dashboard: http://localhost:8761/
- Actuator health check: http://localhost:8761/actuator/health

The Eureka Server does **not** require MySQL or any database connection.

## Secrets

Do not commit real secrets, credentials, or `.env` files. Use `.env.example` as the
template for any environment variables a future service may need; copy it to a local,
git-ignored `.env` and fill in real values there.

## Troubleshooting

**Wrong Java version**

If the build fails with an "unsupported class file version" or similar error, run
`java -version` and confirm it reports major version `21`. Install/select a Java 21 JDK
and ensure it is first on your `PATH` (or configured via `JAVA_HOME`).

**Port 8761 already in use**

If the Eureka Server fails to start with a "port already in use" / bind exception on
`8761`, another process is already using that port. Stop the other process, or find and
stop the process using the port (Windows: `netstat -ano | findstr 8761` then
`taskkill /PID <pid> /F`; Linux/macOS: `lsof -i :8761` then `kill <pid>`).

**Maven dependency download failure**

If `mvnw`/`mvnw.cmd` fails while resolving dependencies, check your network
connection and any corporate proxy/VPN settings, then re-run the build. If a
corporate Maven mirror is required, configure it in your local `~/.m2/settings.xml`
rather than in this repository's POMs.
