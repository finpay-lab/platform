# FinPay Lab — Architecture Audit (Phase 1)

> Audit of the current state of `microservice-lab` (FinPay) against the
> Modernization Mandate: **Java 25 + Spring Boot 4 + production-grade fintech
> platform**. No code changes are made in this phase. See `MODERNIZATION_PLAN.md`
> for the execution path and `TECHNOLOGY_RADAR.md` for adoption decisions.

## 0. Audit scope & method

Inspected (read-only):

- `platform/` shared foundation: `build-logic/` precompiled convention plugins,
  `gradle/libs.versions.toml`, architecture docs (`SYSTEM_OVERVIEW`,
  `ARCHITECTURE`, `SERVICE_CATALOG`, `SECURITY`, `OBSERVABILITY`,
  `PROJECT_PLAN`), and 10 existing ADRs (`adr/0001`–`0010`).
- The 9 repos currently present: `customer-service`, `gateway`,
  `identity-service`, `ledger-service`, `notification-service`, `observability`,
  `transfer-service`, `infrastructure`, `platform`.
- Build files (`*.gradle`, `gradle.properties`) and `AGENTS.md` per repo.

Verified facts (not assumptions):

- JDK target: `JavaLanguageVersion.of(21)` in every service's `build.gradle`.
- Spring Boot: `springBoot = 3.3.5` (in `libs.versions.toml`).
- Gradle: `8.10.2`. PostgreSQL 16, Redis 7.4, Kafka 3.8 (KRaft), OpenSearch
  2.17, Flyway 10, Testcontainers 1.20, OpenTelemetry.
- 8 domain repos were **deleted** (not merely archived) on the user's
  instruction: `account-service`, `wallet-service`, `payment-service`,
  `risk-service`, `limit-service`, `audit-service`, `search-service`,
  `reconciliation-service`. Their GitHub remotes and local clones are gone.

## 1. Current architecture (as-built)

- **Style:** Hexagonal / ports-and-adapters. `domain/` (no Spring/JPA/Kafka
  imports) ↔ `application/` (use cases) ↔ `infrastructure/` (impls) ↔
  `interfaces/` (web/consumers). Enforced by `AGENTS.md` hard rules 1–10 and
  the `quality-conventions` plugin.
- **Data:** Database-per-service. Each service owns its schema + Flyway
  migrations. No shared DB.
- **Consistency:** SAGA + Transactional Outbox + eventual consistency (ADR-0003,
  ADR-0004). No 2PC/XA.
- **Messaging:** Kafka (KRaft, no Zookeeper) for domain events; outbox pattern
  for reliable publish.
- **Edge:** `gateway` module does authN enforcement, routing, rate limiting,
  correlation ID, CORS, threat protection — no business logic.
- **Identity:** `identity-service` integrates an external OIDC IdP, maps
  external subject → internal principal, issues FinPay internal token.
- **Build:** Gradle multi-module composite build; `platform` is the
  foundation (`build-logic/` + shared libraries `common-web`, `common-security`,
  `common-observability`, `common-test`).
- **Observability:** OTel wired; structured logs; Prometheus/Grafana planned
  (P6). Trace context propagation across Kafka is partially implemented.
- **Docs:** Exceptionally strong for a lab — 10 ADRs, context map, transaction
  flows, data ownership, event catalog, security, observability docs.

**Verdict:** The *architecture philosophy* is already production-grade and
aligns with the mandate (bounded contexts, outbox, SAGA, explicit state
machines, idempotency, observability-first). The gap is **version baseline** and
**breadth of implemented domains**, not architectural wrongness.

## 2. Technical debt

| # | Debt | Evidence | Severity |
|---|------|----------|----------|
| D1 | JDK 21, not 25 | `JavaLanguageVersion.of(21)` everywhere | High (mandate) |
| D2 | Spring Boot 3.3.5, not 4 | `springBoot=3.3.5` | High (mandate) |
| D3 | Gradle 8.10.2 | `gradle-8.10.2` | Med (works; bump with SB4) |
| D4 | Testcontainers 1.20 (old) | `libs.versions.toml` | Low |
| D5 | Most services are scaffolding | `identity/notification/transfer/observability/infrastructure` have only 2 Java files each; real domain logic exists mainly in `customer-service` (59), `ledger-service` (24), `gateway` (29) | High (breadth) |
| D6 | OpenSearch 2.17 (EOL-ish) | `libs.versions.toml` | Med |
| D7 | No contract tests, no perf benchmarks, no chaos tests yet | `PROJECT_PLAN` P4/P11 not started | Med |

## 3. Missing production capabilities (vs. mandate)

Mapped to the mandate's phases:

- **Phase 2 (Fintech Domain):** Account, Wallet, Payment, Merchant, Fraud/Risk,
  Reconciliation, Audit, Search are **absent** (deleted). Only Customer, Ledger,
  Transfer, Notification, Identity, Gateway exist. The mandate's domain model
  cannot be demonstrated without recreating these.
- **Phase 3 (Financial correctness):** Ledger double-entry exists (ledger-service)
  but is partial (24 files). Currency/monetary-precision strategy (avoid
  float — use `BigDecimal`/`Money` type) must be verified repo-wide.
- **Phase 4 (Payment processing):** No `payment-service` (deleted). Payment
  state machine, provider failure, webhook/callback, partial failure are not
  implemented in a dedicated service.
- **Phase 5 (Distributed systems):** Outbox worker, DLQ/retry topics, consumer
  replay, backpressure, circuit breakers, bulkheads — partially designed (ADRs)
  but not implemented end-to-end.
- **Phase 6 (API):** OpenAPI, Problem Details (RFC 7807), API versioning,
  pagination, filtering, optimistic concurrency — not uniformly present.
- **Phase 7 (Data):** PostgreSQL source-of-truth OK; read replicas/partitioning
  not designed; Redis used for idempotency/cache/rate-limit (correct — not
  ledger).
- **Phase 8 (Security):** OAuth2/OIDC/RBAC present in design; password hashing,
  key rotation, secrets mgmt, mTLS, WAF, dependency/container scanning — not
  implemented in code/CI.
- **Phase 9 (Observability):** OTel foundation OK; structured-log contract
  (traceId/correlationId/event name) partial; Kafka trace propagation incomplete.
- **Phase 10 (Reliability):** No failure-injection or resilience tests.
- **Phase 11 (Testing):** Unit + some integration; no contract, E2E, resilience,
  or performance (k6/Gatling) suites.
- **Phase 12 (CI/CD):** CI skeleton only; no Trivy/Gitleaks/SBOM/container-scan
  in pipeline.
- **Phase 13 (Infra):** Docker-compose only; no Terraform/K8s/Helm/Argo yet.
- **Phase 14 (Perf):** No measurable targets or benchmarks.

## 4. Unnecessary complexity

- **`observability` repo (2 files):** A near-empty repo. Observability should
  likely live as shared config/libs in `platform` + collector manifests, not a
  separate "service" with no runtime. Candidate to fold into `platform` or
  `infrastructure`.
- **`infrastructure` repo (2 files):** Similarly skeletal; fine as an IaC home
  but currently a stub.
- **`gateway` as a module vs. standalone repo:** Currently a module under
  `platform`? Verify — if it pulls the whole platform build, that complicates
  independent deployment. (See `SERVICE_CATALOG`: gateway listed as "separate
  module".) For K8s per-service deploy, gateway should be its own deployable.

## 5. Outdated dependencies

- Spring Boot 3.3.5 → 4.x (mandate). Pulls Spring Framework 6.1 → 7, Jakarta EE
  10 → 11.
- Java 21 → 25 (mandate; Java 25 is an LTS release).
- OpenSearch 2.17 → 2.x latest / 3.x (check compatibility with Spring Data).
- Testcontainers 1.20 → 1.2x (improved Docker/OTel support).
- Flyway 10 → 11 (if PG 17 adopted).
- Kafka client 3.8 → latest 3.9/4.x (verify Spring Kafka compatibility).

## 6. Security risks

- **Secrets:** No secrets-management impl; `.autocoding/` scratch + agent memory
  files present in repos (`.autocoding/MEMORY.md`) — ensure no secrets committed
  (Gitleaks must be added to CI).
- **AuthZ:** RBAC designed (identity-service) but not enforced end-to-end at
  service boundaries (gateway delegates; do services re-check?).
- **Transport:** mTLS service-to-service not implemented.
- **Supply chain:** No SBOM, no dependency/container scanning → vulnerable
  transitive deps can ship.
- **Injection/SSRF:** Not explicitly tested; need security unit tests + ZAP/SAST.

## 7. Scalability risks

- **Ledger hot path:** Balance calculation per request is O(entries) unless
  materialized (balance projection / cached balance). Must add a balance table
  updated on post.
- **Kafka consumer lag:** No consumer-lag SLO or autoscaling (HPA on lag) yet.
- **DB connection pools:** Undersized pools + virtual threads can exhaust
  connections (virtual threads are I/O-bound; JDBC is blocking → need
  virtual-thread-friendly pool sizing or Project Loom JDBC offloading).
- **Idempotency store:** Redis-based; must survive Redis outage (fallback to DB
  unique constraints).

## 8. Reliability risks

- **Outbox publisher:** If the worker dies between DB-commit and publish, events
  are lost until replay. Need a poller with marked-published + DLQ.
- **SAGA compensation:** transfer-service compensation/recovery partially
  implemented; crash during compensate must be safe (idempotent compensation).
- **Poison messages:** No DLQ wiring verified in code.
- **Partial deploy:** No readiness/liveness probes, no graceful shutdown yet
  (needed before K8s).

## 9. Key decision required before Phase 2 (deleted services)

The 8 deleted repos are **exactly the domains the mandate demands** (Account,
Payment, Risk/Fraud, Reconciliation, Audit, Wallet, Search, Limit). With them
gone, the lab cannot demonstrate Phases 2–5 of the mandate. Before any
implementation, decide:

- **(A) Recreate the deleted services** from the mandate's domain model (clean
  Java 25/SB4 implementations, not restored old Java 21 code). Recommended if
  the goal is a full fintech demonstration.
- **(B) Proceed with the 9 surviving repos**, accepting a narrower domain
  (Customer, Ledger, Transfer, Notification, Identity, Gateway + platform/infra/
  observability) and trimming the mandate's domain scope accordingly.

This decision drives the entire modernization plan (see `MODERNIZATION_PLAN.md`,
§Decision Gate 0).

## 10. Audit conclusion

The architecture is **sound and worth keeping**; the work is a **baseline
upgrade (Java 21→25, SB 3.3.5→4) + breadth completion (recreate deleted
domains) + production hardening (security/observability/reliability/CI/CD)**,
not a rewrite. Do not discard the hexagonal/outbox/SAGA foundation — extend it.
