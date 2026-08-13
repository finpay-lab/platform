# Project Plan

Living plan for `microservice-lab` (FinPay). Hermes = Architect/Orchestrator;
OpenCode/engineer = implementation executor. Review loop after every task
(§49). Repository kept continuously buildable (§58).

## Phases (refined from §46)

Each phase = a vertical slice, verified (build + tests) before the next.

| Phase | Theme | Delivers |
|-------|-------|----------|
| **P0** | Foundation | Repo, Gradle multi-module, build-logic conventions, shared libs (web/security/observability/test), local docker-compose infra, CI skeleton, **this architecture doc set** |
| **P1** | Identity + Customer + Gateway | identity-service (OIDC integration), customer-service (profile/status/KYC), gateway routing + JWT enforcement |
| **P2** | Ledger | ledger-service: double-entry immutable postings, balance calc, reversal, DB invariants |
| **P3** | Transfer + SAGA | transfer-service: orchestrated SAGA, compensation, crash recovery |
| **P4** | Kafka + Outbox hardening | outbox publisher worker, DLQ/retry topics, consumer idempotency, event ordering tests |
| **P5** | Notification | notification-service (email/sms/push async, failure-isolated) |
| **P6** | Observability | Prometheus rules, Grafana dashboards, OTel collector, tracing across saga |
| **P7** | Kubernetes | manifests, probes, HPA, PDB, NetworkPolicy, RBAC |
| **P8** | Helm + Argo CD | reusable charts, values-*, GitOps app |
| **P9** | Terraform + Ansible | IaC modules, envs, VM bootstrap |
| **P10** | Security + WAF | WAF rules, mTLS, scanning in CI, secret handling |
| **P11** | Failure + Performance | chaos/failure injection tests (§37), load tests (§53) |
| **P12** | Architecture hardening | full quality-gate review, docs finalization |

> **Scope reduction (2026-08-13).** Trimmed from 16 → **6 learning
> services** (one per distinct case). Archived repos (reversible):
> account-service, wallet-service, payment-service, risk-service,
> limit-service, audit-service, search-service, reconciliation-service. See
> `SERVICE_CATALOG.md`.

## Dependency graph (abridged)

```
P0 ─► P1 ─► P2 ─► P3 ─► P4
                       │
                       ├─► P5 (notification)
P0/P4 ─► P6 (observability can land incrementally per service too)
P1+ ─► P7 ─► P8 ─► P9
P0+ ─► P10
P3/P4 ─► P11
all ─► P12
```

Detailed task list with IDs in `PROJECT_TASKS.md`.

## Definition of Done (per task, §50)

Code + tests + passing + architecture respected + security/observability
considered + error handling + docs updated + migrations added + contracts
updated + no unrelated changes.

## Status tracking

`PROJECT_TASKS.md` holds the canonical task table (ID, status, deps, AC).
