# Local Development

Goal: the **entire platform runs locally** with one command (§45). Business
services may run via Gradle/IDE or containers; infrastructure always runs in
containers.

## Prerequisites

- Docker + Docker Compose
- JDK 21 (use the repo's `sdk`/`asdf`/CI image; or `./gradlew` which carries its
  own toolchain via Gradle)
- Gradle 8.10.x (wrapper provided)
- `make` (optional convenience)

## Local infrastructure (`docker compose`)

`docker-compose.yml` (repo root) brings up:

| Component | Purpose |
|-----------|---------|
| PostgreSQL (one container per service DB, or schemas per service) | transactional stores |
| Apache Kafka (KRaft) + UI | event backbone |
| Redis | idempotency, rate-limit, cache |
| OpenSearch + Dashboards | search |
| Keycloak (IdP) | OIDC |
| Prometheus + Grafana + OTel Collector | observability |

> Database-per-service is logical: locally we run **separate schemas/databases**
> per service on shared Postgres containers to keep it light, while prod uses
> separate instances (Terraform). The *ownership* rule is unchanged.

## Running services

- **All in containers:** `make up` (builds images, runs everything).
- **Mixin:** run one service from IDE (`bootRun`) against the composed infra.
  Each service's `application.yml` reads infra hosts from env (default
  `localhost`).

## First-run setup

1. `docker compose up -d` — wait for healthy.
2. `./gradlew :services:identity-service:bootRun` (etc.) or `make services`.
3. Seed Keycloak realm (`scripts/seed-keycloak.sh`) — creates FinPay realm,
   client, roles.
4. Open Grafana (`localhost:3000`), Kafka UI, OpenSearch Dashboards.

## Useful commands

- `./gradlew build` — build + test all modules.
- `./gradlew :services:ledger-service:test` — single module.
- `docker compose down -v` — full reset.
- `make reset-db` — re-run Flyway from clean.

## Profiles

`dev` (local compose), `staging`, `prod` (K8s). `values-*.yaml` in Helm mirror
these.

## Tips

- Outbox publisher logs pending count; if Kafka is down, watch events queue and
  recover when it returns (demonstrates §37 "DB commit, Kafka fails").
- Use `scripts/` for seeding, topic creation, load tests.
