# Project Plan

Living plan for `microservice-lab` (FinPay). Hermes = Architect/Orchestrator;
OpenCode/engineer = implementation executor. Review loop after every task
(§49). Repository kept continuously buildable (§58).

## Phases (refined from §46)

Each phase = a vertical slice, verified (build + tests) before the next.

| Phase | Theme | Delivers |
|-------|-------|----------|
| **P0** | Foundation | Repo, Gradle multi-module, build-logic conventions, shared libs (web/security/observability/test), local docker-compose infra, CI skeleton, **this architecture doc set** |
| **P1** | Identity + Customer + Account | identity-service (OIDC integration), customer-service (profile/status/KYC), account-service (lifecycle/status), gateway routing + JWT enforcement |
| **P2** | Ledger | ledger-service: double-entry immutable postings, balance calc, reversal, DB invariants |
| **P3** | Payment | payment-service: state machine, idempotent creation, outbox |
| **P4** | Transfer + SAGA | transfer-service: orchestrated SAGA, compensation, crash recovery |
| **P5** | Kafka + Outbox hardening | outbox publisher worker, DLQ/retry topics, consumer idempotency, event ordering tests |
| **P6** | Risk + Limits | risk-service (gRPC sync eval), limit-service (concurrency-safe usage) |
| **P7** | Notification | notification-service (email/sms/push async, failure-isolated) |
| **P8** | Search | search-service + OpenSearch indexer, CQRS read side |
| **P9** | Reconciliation + Audit | reconciliation (ledger vs payment/transfer), audit (immutable) |
| **P10** | Observability | Prometheus rules, Grafana dashboards, OTel collector, tracing across saga |
| **P11** | Kubernetes | manifests, probes, HPA, PDB, NetworkPolicy, RBAC |
| **P12** | Helm + Argo CD | reusable charts, values-*, GitOps app |
| **P13** | Terraform + Ansible | IaC modules, envs, VM bootstrap |
| **P14** | Security + WAF | WAF rules, mTLS, scanning in CI, secret handling |
| **P15** | Failure + Performance | chaos/failure injection tests (§37), load tests (§53) |
| **P16** | Architecture hardening | full quality-gate review, docs finalization |

## Dependency graph (abridged)

```
P0 ─► P1 ─► P2 ─► P3 ─► P4 ─► P5
                       │
                       ├─► P6 ─► P7
                       ├─► P8
                       ├─► P9
P0/P5 ─► P10 (observability can land incrementally per service too)
P1+ ─► P11 ─► P12 ─► P13
P0+ ─► P14
P4/P5 ─► P15
all ─► P16
```

Detailed task list with IDs in `PROJECT_TASKS.md`.

## Definition of Done (per task, §50)

Code + tests + passing + architecture respected + security/observability
considered + error handling + docs updated + migrations added + contracts
updated + no unrelated changes.

## Status tracking

`PROJECT_TASKS.md` holds the canonical task table (ID, status, deps, AC).
