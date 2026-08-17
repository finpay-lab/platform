# FinPay Lab — Modernization Plan (Phase 1 → execution)

Goal: evolve `microservice-lab` from **Java 21 / Spring Boot 3.3.5** to a
**Java 25 / Spring Boot 4** production-grade fintech platform per the mandate,
while **preserving the sound hexagonal / outbox / SAGA foundation** (do not
rewrite what works).

This document is the execution map. It does NOT change code. Phase 1 (this
audit) is complete once these three docs land. Implementation begins only after
**Decision Gate 0** is resolved.

## Decision Gate 0 — deleted services (BLOCKER for breadth)

The 8 deleted repos (`account, wallet, payment, risk, limit, audit, search,
reconciliation`) are exactly the domains the mandate's Phases 2–5 require. Two
paths:

- **Path A — Recreate (recommended for a full fintech demo).** Build them fresh
  on Java 25/SB4 from the mandate's domain model. Do NOT restore the old Java 21
  code (it's deleted; a clean reimplementation is the point). Adds ~8 services.
- **Path B — Proceed narrow.** Keep the 9 surviving repos; trim the mandate's
  domain scope to Customer/Ledger/Transfer/Notification/Identity/Gateway. Document
  the reduced scope explicitly.

**This gate must be answered before Phase 2.** The plan below is written for
**Path A** (full domain); if Path B, drop the "recreate" workstreams.

## Baseline upgrade (Path-agnostic, do first)

Get the version jump green on the *existing* 9 repos before adding breadth.
This de-risks everything else.

| Step | Action | Verify |
|------|--------|--------|
| B1 | Bump `platform/build-logic` + `libs.versions.toml`: Java 25, SB 4 (or SB 3.4.x interim if SB4 not GA), Gradle 8.14+, compatible Spring Security/Data/Kafka 4.x, Flyway 11, Testcontainers 1.2x | `gradle :platform:compileJava` green |
| B2 | `javax→jakarta` namespace, actuator/config-prop changes for SB4 | each service compiles |
| B3 | Enable virtual threads (`spring.threads.virtual.enabled=true`); size DB pools for blocking JDBC | load test ledger read path |
| B4 | Gradle composite build stays green; precompiled convention plugins updated | `gradle build` on all 9 |

ADRs to add/update: **ADR-0011 Java 25**, **ADR-0012 Spring Boot 4** (update
0001), **ADR-0013 Virtual Threads for JDBC**, **ADR-0014 Money/BigDecimal
precision policy** (formalize "no float").

## Phased execution (maps to mandate phases)

Each phase = vertical slice, build+test green before next. One Linear issue per
slice; the automation pipeline (coding-automation) drives `loop run FP-X --repo`
per service (repo mapping required for recreated services).

### P2 — Fintech domain (recreate deleted services, Path A)
- `account-service` (ownership/balance per customer, distinct from ledger),
  `wallet-service` (if kept distinct from ledger — else fold into account),
  `payment-service` (payment state machine, provider failure, webhook, partial
  failure — the mandate's Phase 4 centerpiece), `risk-service` (gRPC sync eval),
  `limit-service` (spending limits), `reconciliation-service` (ledger vs
  payment/transfer mismatch), `audit-service` (immutable WHO/WHAT/WHEN),
  `search-service` (OpenSearch CQRS read model).
- ADR: **ADR-0015 Service boundaries (recreated set)** — justify each bounded
  context against the earlier "redundancy" critique (the deletion argued these
  overlapped; the mandate now demands them — resolve the tension explicitly).

### P3 — Financial correctness (harden, all services)
- Immutable postings, debit/credit invariants, balance projection table,
  currency as `BigDecimal`/`Money`, rounding rules, reversal, audit trail.
- ADR-0014 (Money). Tests: determinism, idempotency, duplicate-event.

### P4 — Payment processing
- `payment-service`: idempotency keys, duplicate requests, state machine
  (Requested→Pending→Processing→Succeeded→Ledger→Reconciled), retry/timeout,
  provider failure, webhook/callback, partial failure — **implemented + tested**,
  not documented.

### P5 — Distributed systems
- Transactional Outbox worker (poll + publish + mark), DLQ/retry topics,
  consumer idempotency (by `eventId`), consumer replay, ordering, lag SLO,
  backpressure, circuit breakers (Resilience4j), bulkheads, rate limiting.
- ADR-0004 (outbox) already exists; add operational runbook.

### P6 — API architecture
- OpenAPI on every external API, RFC 7807 Problem Details, versioning
  (`/v1`), pagination/filtering, validation, idempotency header, optimistic
  concurrency (ETag/`@Version`), consistent errors, rate limiting at gateway.

### P7 — Data hardening
- PG transaction isolation review, indexing, query optimization, connection
  pooling for virtual threads, optimistic/pessimistic where justified,
  partitioning for ledger (by time/account), read replicas where justified.
- Redis: idempotency/cache/rate-limit only; DB unique-constraint fallback.

### P8 — Security
- Spring Security 7 resource server (JWT/OIDC), RBAC enforced at service edge,
  service-to-service mTLS, secrets mgmt (Vault or env+KMS), Argon2id if local
  users, key rotation, WAF rules, **CI security scans**: dependency (OWASP
  Dependency-Check / Snyk), container (Trivy), secrets (Gitleaks), SAST.
- ADR-0016 Security architecture.

### P9 — Observability
- OTel traces across Kafka (context propagation verified), structured logs
  (traceId/correlationId/event name, **no secrets**), metrics (request/error
  rate, p50/p95/p99, DB pool, Kafka lag, payment success/failure rate,
  processing time), Prometheus rules + Grafana dashboards.

### P10 — Reliability
- Failure-injection tests (DB/Kafka/Redis outage, timeout, network, duplicate
  event, poison message, consumer crash, stale cache, partial deploy). For each:
  detection→containment→recovery→reconciliation→observability.

### P11 — Testing pyramid
- Unit (domain), Integration (Testcontainers PG/Kafka/Redis), Contract
  (Pact for service boundaries), E2E (critical payment flow), Resilience
  (dependency failure + retry), Performance (k6/Gatling: throughput, p95/p99,
  error rate, CPU/mem, DB util). Never claim perf without numbers.

### P12 — CI/CD
- GitHub Actions: compile→unit→static (Spotless/PMD)→integration→security
  scan→container build→Trivy→Gitleaks→SBOM (Syft/CycloneDX)→artifact→deploy→
  smoke. Image signing if registry supports it.

### P13 — Infrastructure (justified)
- Docker (done) → Terraform (envs, AWS) → K8s **only if** >6 deployables need
  HPA/PDB: readiness/liveness probes, graceful shutdown, requests/limits, HPA,
  rolling, PDB, NetworkPolicy, RBAC, secrets, observability.

### P14 — Performance engineering
- Measurable targets per critical API (throughput, p95, p99, error rate).
  Benchmark before/after virtual threads, pooling, query, Kafka, caching, GC.

### P15 — ADRs
- Maintain `adr/`; every significant decision gets Context/Problem/
  Alternatives/Decision/Trade-offs/Consequences (template already in repo).

### P16 — Technology radar
- Re-review `TECHNOLOGY_RADAR.md` each phase; promote TRIAL→ADOPT on green
  pilot + measurements.

### P17 — AI engineering
- AI assists coding/test/doc/review/incident; **never** authoritative for
  balances/ledger/authZ/financial transitions. Financial logic stays
  deterministic (ADR-0014 + guardrail in `AGENTS.md`).

## Definition of Done (per task, from mandate)
Implementation + unit tests + integration tests (where apt) + failure-scenario
tests + security review + observability + migration (if needed) + API docs +
ADR update + perf verified (where apt) + CI green + docs updated.

## Sequencing summary

```
Gate0 (recreate? ) ─┐
                    ├─► B1–B4 baseline upgrade (9 repos, Java25/SB4)
                    │      │
                    │      ▼
                    │   P2 (domains) ─► P3 (correctness) ─► P4 (payment)
                    │      │                                    │
                    │      ▼                                    ▼
                    └─► P5 (distributed) ─► P6 (API) ─► P7 (data)
                           │
                           ▼
                        P8 (security) ─► P9 (obs) ─► P10 (reliability)
                           │
                           ▼
                        P11 (test) ─► P12 (CI/CD) ─► P13 (infra)
                           │
                           ▼
                        P14 (perf) ─► P15/P16 (ADR/radar) ─► P17 (AI guardrails)
```

## Immediate next step
Resolve **Decision Gate 0**. If Path A, I will (a) map each recreated service to
its repo, (b) create the Linear issues, (c) drive them via the automation
pipeline. If Path B, I will trim the mandate scope and start B1–B4 on the 9
surviving repos.
