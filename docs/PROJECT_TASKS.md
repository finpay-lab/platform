# Project Tasks (dependency graph)

Tasks follow the OpenCode task format (§48). Status: `PLANNED` | `IN_PROGRESS`
| `DONE` | `CHANGES_REQUIRED` | `BLOCKED`.

Legend for dependency arrows: `TASK-xxx → TASK-yyy` means yyy depends on xxx.

## P0 — Foundation

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-002 | build-logic convention plugins (java/spring/test/quality) | TASK-001 | DONE |
| TASK-003 | Shared lib: common-web (problem details, correlation, pagination) | TASK-002 | DONE |
| TASK-004 | Shared lib: common-security (JWT auth, RBAC, principal) | TASK-002 | DONE |
| TASK-005 | Shared lib: common-observability (OTel, metrics, tracing) | TASK-002 | DONE |
| TASK-006 | Shared lib: common-test (Testcontainers base, ArchUnit rules) | TASK-002 | DONE |
| TASK-007 | docker-compose local infra (PG/Kafka/Redis/OpenSearch/Keycloak/Prom) | TASK-001 | DONE |
| TASK-008 | CI skeleton (build/test/scan) GitHub Actions | TASK-002 | DONE |
| TASK-009 | `contracts/` event + api contract stubs + versioning policy | TASK-001 | PLANNED |
|----|-------|------|--------|
| TASK-001 | Repo skeleton + Gradle multi-module + settings | — | DONE |
| TASK-002 | build-logic convention plugins (java/spring/test/quality) | TASK-001 | PLANNED |
| TASK-003 | Shared lib: common-web (problem details, correlation, pagination) | TASK-002 | PLANNED |
| TASK-004 | Shared lib: common-security (JWT auth, RBAC, principal) | TASK-002 | PLANNED |
| TASK-005 | Shared lib: common-observability (OTel, metrics, tracing) | TASK-002 | PLANNED |
| TASK-006 | Shared lib: common-test (Testcontainers base, ArchUnit rules) | TASK-002 | PLANNED |
| TASK-007 | docker-compose local infra (PG/Kafka/Redis/OpenSearch/Keycloak/Prom) | TASK-001 | PLANNED |
| TASK-008 | CI skeleton (build/test/scan) GitHub Actions | TASK-002 | PLANNED |
| TASK-009 | `contracts/` event + api contract stubs + versioning policy | TASK-001 | PLANNED |

## P1 — Identity + Customer + Account + Gateway

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-010 | identity-service: OIDC integration, principal/role mapping | TASK-003,004,007 | PLANNED |
| TASK-011 | customer-service: profile, status state machine, KYC | TASK-003,004,006,007 | PLANNED |
| TASK-012 | account-service: account aggregate, lifecycle, status | TASK-003,004,006,007 | PLANNED |
| TASK-013 | gateway: routing, JWT enforcement, rate-limit, correlation | TASK-004,005,007,010 | PLANNED |

## P2 — Ledger

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-020 | ledger-service: double-entry postings, immutable entries, reversal | TASK-003,004,006,007 | PLANNED |
| TASK-021 | ledger: balance calc + optimistic locking, DB invariant `SUM(d)=SUM(c)` | TASK-020 | PLANNED |
| TASK-022 | ledger: outbox publish `LedgerEntryPosted` | TASK-020,005,009 | PLANNED |

## P3 — Payment

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-030 | payment-service: payment aggregate + state machine | TASK-003,004,006,007 | PLANNED |
| TASK-031 | payment: idempotent creation (idempotency store) | TASK-030,005,006 | PLANNED |
| TASK-032 | payment: outbox `PaymentCreated/Completed/Failed` | TASK-030,009 | PLANNED |

## P4 — Transfer + SAGA

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-040 | transfer-service: transfer aggregate + saga state | TASK-011,012,020,030 | PLANNED |
| TASK-041 | transfer: orchestrated SAGA (validate→limit→risk→reserve→debit→credit→finalize) | TASK-040,021,031 | PLANNED |
| TASK-042 | transfer: compensation + crash-recovery (persisted saga state) | TASK-041 | PLANNED |
| TASK-043 | transfer: idempotent creation | TASK-040,031 | PLANNED |

## P5 — Kafka + Outbox hardening

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-050 | outbox publisher worker (poll + publish + mark) | TASK-022,032 | PLANNED |
| TASK-051 | retry/DLQ topics + consumer idempotency + poison handling | TASK-050,009 | PLANNED |
| TASK-052 | Kafka duplicate + out-of-order consumer tests | TASK-051 | PLANNED |

## P6 — Risk + Limits

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-060 | risk-service: gRPC sync eval (velocity/amount/country/device) | TASK-004,006,007,013 | PLANNED |
| TASK-061 | limit-service: concurrency-safe usage accounting | TASK-004,006,007 | PLANNED |

## P7 — Notification

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-070 | notification-service: async email/sms/push, failure-isolated | TASK-003,005,009 | PLANNED |

## P8 — Search

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-080 | search-service + OpenSearch indexer (CQRS read side) | TASK-005,009,007 | PLANNED |

## P9 — Reconciliation + Audit

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-090 | reconciliation-service: ledger vs payment/transfer mismatch detection | TASK-020,030,040 | PLANNED |
| TASK-091 | audit-service: immutable WHO/WHAT/WHEN/... records | TASK-005,009 | PLANNED |

## P10 — Observability

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-100 | Prometheus rules + Grafana dashboards + OTel across saga | TASK-005 | PLANNED |

## P11–P16

| ID | Title | Deps | Status |
|----|-------|------|--------|
| TASK-110 | K8s manifests (probes/HPA/PDB/NetworkPolicy/SA) | TASK-013+ | PLANNED |
| TASK-120 | Helm charts + values-* + Argo CD Application | TASK-110 | PLANNED |
| TASK-130 | Terraform modules + envs; Ansible bootstrap | TASK-110 | PLANNED |
| TASK-140 | WAF rules + mTLS + CI scanning | TASK-110 | PLANNED |
| TASK-150 | Failure injection (§37) + load tests (§53) | TASK-051,042 | PLANNED |
| TASK-160 | Architecture hardening + final quality-gate review | all | PLANNED |

## Dependency highlights

- Nothing business lands before P0 foundation (build system + shared libs +
  infra + CI).
- Transfer (P4) is the integration crux: depends on customer(P1), account(P1),
  ledger(P2), payment(P3), and consumes risk/limit (P6) — so P6 may land
  partially before P4's risk step; the SAGA tolerates risk-service being a stub.
- Observability (P10) is applied incrementally per service too, not only at P10.
