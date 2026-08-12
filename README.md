# finpay/platform (FinPay)

> **Repo identity (TASK-014):** this repository is `finpay-lab/platform` — the
> FinPay engineering **platform** repo. It owns the shared engineering libraries
> (`libraries/common-*`), all architecture docs (`docs/`), the contracts
> (`contracts/`), and the build conventions. Each business service lives in its
> own repository (one repo per service, ADR-0011) and consumes the
> `com.finpay:common-*` libraries from GitHub Packages.

A fintech **distributed-systems laboratory**. The point is to demonstrate *why*
architecture decisions exist — SAGA + Outbox, database-per-service, idempotency,
CQRS, event-driven payment flows — not to ship a product.

This repository is **P0: the executable foundation** (TASK-001): a buildable
Gradle multi-module skeleton with shared convention plugins and common libraries.
No business logic yet.

## Architecture

The complete design lives in `docs/` (start with `SYSTEM_OVERVIEW.md` and
`ARCHITECTURE.md`, then `PROJECT_TASKS.md` for the build order). Ten hard
architecture rules are enforced in review; the most important:

1. No shared database — each service owns its schema + Flyway migrations (ADR-0005).
2. No distributed transactions — SAGA + Outbox + eventual consistency (ADR-0002/3/4).
3. Domain logic is independent of infrastructure (no Spring/JPA/Kafka in `domain/`).
4. Every financial operation and async consumer defines idempotency.

## Repository layout

```
build-logic/   precompiled Gradle convention plugins (java/spring/testing/quality)
gradle/        version catalog (libs.versions.toml) + wrapper
libraries/     shared engineering libraries
  common-web           RFC-9457 problem details, correlation id
  common-security      roles/RBAC primitives
  common-observability W3C trace context helpers
  common-test          shared ArchUnit architecture rules
services/      business services — land in P1+ (empty by design)
contracts/     event + API contract stubs (P0)
docs/          architecture, ADRs, task specs
docker-compose.yml  local infra: Postgres, Kafka (KRaft), Redis, OpenSearch,
                    Keycloak, Prometheus, Grafana, OTel Collector
.github/workflows/   CI skeleton
```

## Build

No JDK needed locally — build through the pinned Gradle Docker image
(8.10.2-jdk21, ADR-0001):

```bash
docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle \
  gradle:8.10.2-jdk21 gradle projects

docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle \
  gradle:8.10.2-jdk21 gradle clean build
```

`gradle build` must stay green on every commit. Because the Docker volume
mangles file mtimes, run `clean` before rebuilds.

## Local infrastructure

```bash
docker compose up -d     # Postgres, Kafka, Redis, OpenSearch, Keycloak, observability
docker compose config    # validate without starting
docker compose down -v   # full reset
```

See `docs/LOCAL_DEVELOPMENT.md` for the run book and first-run setup.

## Tech baseline (ADR-0001)

Java 21 LTS · Spring Boot 3.3.5 · Gradle 8.10.2 · PostgreSQL 16 · Redis 7.4 ·
Kafka 3.8 (KRaft) · OpenSearch 2.17 · Flyway 10 · Testcontainers 1.20 ·
OpenTelemetry. Do NOT bump these without an ADR.

## Contributing to this lab

Tasks follow the OpenCode task format in `docs/tasks/` (status tracked in
`docs/PROJECT_TASKS.md`). Implement one focused task at a time; keep the repo
buildable; no business logic before P0 is complete.
