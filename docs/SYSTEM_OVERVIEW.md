# FinPay — System Overview

`microservice-lab` is a **fintech distributed-systems laboratory**. It is a
fictional platform, **FinPay**, built to demonstrate senior-level backend and
distributed-systems engineering under realistic financial constraints: money
must not be lost or double-spent, failures are the norm not the exception, and
correctness must hold under concurrency and partial failure.

This is not a tutorial collection. Every service, technology, and pattern exists
**because a concrete requirement forces it**. See `docs/adr/` for the
justification of each significant decision.

## What the platform does

FinPay lets a customer register, authenticate, hold accounts and wallets, move
money (internal and external transfers), and see their history — while a set of
supporting services evaluate risk, enforce limits, notify the customer, audit
every sensitive action, and reconcile the books.

## The three questions this repo must answer

A reader should be able to trace, for every component:

1. **Why does it exist?** (problem it solves)
2. **What happens when it fails?** (resilience + compensation)
3. **What is the source of truth, and who owns it?** (data ownership)

## Core engineering properties

- **Database-per-service.** No shared database. Each service owns its schema and
  migrations (Rule 1, §41).
- **No distributed transactions by default.** SAGA + Outbox + eventual
  consistency instead of 2PC/XA (Rule 2).
- **Every financial operation is idempotent** (Rule 6) and every async consumer
  tolerates duplicates/out-of-order (Rule 7).
- **State is explicit.** Financial entities use typed state machines with
  documented legal transitions, never ad-hoc booleans (§9).
- **Observability is not optional.** Every request carries `traceId`,
  `spanId`, `correlationId`; metrics, logs, and traces are first-class.

## Runtime shape (high level)

```
                 Internet
                    │
                 WAF (§29)
                    │
        Load Balancer / Ingress
                    │
              API Gateway  (authN enforcement, routing, rate limit, correlation)
                    │
   ┌────────────────┼───────────────────────────────────────────┐
   │                │                                            │
Identity Provider   │         Microservices (each owns its DB)    │
   (OIDC)           │   customer · account · wallet · ledger ·    │
                    │   payment · transfer · risk · limit ·       │
                    │   notification · reconciliation · audit ·    │
                    │   search                                    │
                    └──────────────┬─────────────────────────────┘
                                   │ Kafka (events, outbox, DLQ)
                  ┌────────────────┼────────────────┐
              OpenSearch      Redis (idempotency,   Prometheus /
              (search,        rate limit, cache)    Grafana / OTel
              not source)
```

## Document map

| Doc | Purpose |
|-----|---------|
| `ARCHITECTURE.md` | Tactical/hexagonal structure, layering, rules |
| `SERVICE_CATALOG.md` | Every service, responsibility, data, tech |
| `DATA_OWNERSHIP.md` | Who owns what data, why |
| `EVENT_CATALOG.md` | Kafka topics, keys, schemas, DLQ |
| `SECURITY.md` | OAuth/OIDC, RBAC, secrets, mTLS, WAF |
| `OBSERVABILITY.md` | Metrics, tracing, logging, dashboards |
| `LOCAL_DEVELOPMENT.md` | How to run the whole platform locally |
| `adr/` | Architecture Decision Records |
| `architecture/` | Context map, deployment, runtime, transaction flows |
| `PROJECT_PLAN.md`, `PROJECT_TASKS.md` | Roadmap + task dependency graph |

## Buildability

The repository is kept **continuously buildable**. Each phase adds a vertical
slice and is verified before the next begins (§58). The first deliverable
(Phase 0) is architecture + foundation only — no business logic yet.
