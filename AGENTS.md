# AGENTS.md — microservice-lab (FinPay)

Autonomous coding agent guidance. Read this before implementing.

## What this repo is

A fintech distributed-systems **laboratory** (fictional platform "FinPay").
The point is to demonstrate *why* architecture decisions exist, not to ship a
product. Quality of engineering reasoning matters more than feature count.

## Hard architecture rules (enforced in review)

1. **No shared database.** Each service owns its DB/schema + Flyway migrations.
2. **No distributed transactions (2PC/XA)** by default. Use SAGA + Outbox +
   eventual consistency.
3. **No business logic in controllers.** Controllers map transport ↔ use case.
4. **Domain logic independent of infrastructure** (no Spring/JPA/Kafka imports
   in `domain/`). Repositories are interfaces in `domain/`; impls in
   `infrastructure/`.
5. **No remote calls inside `@Transactional`.** Persist+commit, then publish.
6. **Every financial operation defines idempotency** (idempotency key).
7. **Every async consumer defines duplicate/out-of-order behavior** (idempotent
   by `eventId`).
8. **Every remote dependency defines timeout/retry/circuit-breaker.**
9. **Every state machine defines legal transitions**; reject invalid ones.
10. **Every architectural shortcut must be documented** (ADR or comment).

## Tech baseline (ADR-0012, supersedes ADR-0001)

Java 25 LTS · Spring Boot 4.1.0 · Gradle 8.14.x · PostgreSQL 16 · Redis 7.4 ·
Kafka 3.8 (KRaft) · OpenSearch 2.17 · Flyway 11 · Testcontainers 1.21 ·
OpenTelemetry. See `docs/adr/0012-java25-springboot4-baseline.md`. Versions are
centralized in `gradle/libs.versions.toml`; bump there + the `build-logic`
convention plugins, never per-service.

## Build system

- Multi-module Gradle. `build-logic/` = precompiled Groovy convention plugins:
  `com.finpay.java-conventions`, `com.finpay.spring-conventions`,
  `com.finpay.testing-conventions`, `com.finpay.quality-conventions`.
- Versions centralized in `gradle/libs.versions.toml` (use `libs.*` accessors,
  not raw strings, inside precompiled plugins).
- Each module applies conventions; libraries set `bootJar { enabled = false }`,
  services leave it enabled.
- Always run builds via the Gradle Docker image (no JDK locally):
  `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.14.5-jdk21-ubi gradle <task>`
  (Spring Boot 4.x requires Gradle 8.14+; the older `gradle:8.10.2-jdk21`
  image fails with "Spring Boot plugin requires Gradle 8.x (8.14 or later)".)
  Because the Docker volume mangles file mtimes, include `:module:clean` (or
  full `clean`) before a build so Gradle recompiles correctly.

## Documentation

Architecture lives in `docs/` (SYSTEM_OVERVIEW, ARCHITECTURE, SERVICE_CATALOG,
DATA_OWNERSHIP, EVENT_CATALOG, SECURITY, OBSERVABILITY, LOCAL_DEVELOPMENT,
adr/*, architecture/*, PROJECT_PLAN, PROJECT_TASKS). Read the relevant doc
before touching a service. Task specs live in `docs/tasks/`.

## Working model (this lab)

Hermes = Architect/Orchestrator; the coding agent = Implementation Executor.
You implement one focused task at a time (OpenCode task format in
`docs/tasks/`). Do NOT redesign unrelated modules. Keep the repo buildable.

## Critical sandbox note

This environment's OpenCode sandbox **auto-rejects writes outside the repo
directory**. Therefore:
- **Work only inside this repository directory.** Never create scratch
  Gradle projects or temp files under `/tmp` to "test" plugins — that triggers
  the auto-reject and kills the run.
- To verify convention plugins, build the real in-repo modules instead.
- Run `opencode run` with `--auto` so in-repo permission prompts are approved.
