# Architecture

This document defines the **mandatory structural rules** every service follows,
plus the rationale. It is the contract OpenCode/engineers implement against.

## Structural style: Hexagonal + Clean

Every non-trivial service uses this package layout (§43):

```
<service>/
  src/main/java/.../
    domain/        # entities, value objects, aggregates, domain events, repo interfaces, domain services
    application/   # use cases (commands/queries), orchestration, ports (interfaces to infra)
    infrastructure/# persistence (JPA), messaging (Kafka), external clients (REST/gRPC)
    interfaces/    # REST controllers, Kafka listeners, gRPC servers — transport only
  src/test/java/...
```

Rules enforced (§41):

- **R3 — No business logic in controllers.** Controllers translate
  transport → application use case and back. No `@Transactional` on controllers.
- **R4 — Domain independent of infrastructure.** Domain code has **zero**
  imports from Spring, JPA, Kafka. Repositories are interfaces in `domain`;
  implementations live in `infrastructure`.
- **R5 — No remote calls inside `@Transactional`.** A DB transaction must not
  wrap a `RestTemplate`/`KafkaTemplate`/`WebClient` call. Persist, commit, then
  publish (see Outbox, ADR-0004).
- **R6 — Every financial operation defines idempotency.**
- **R7 — Every async consumer defines duplicate/out-of-order behavior.**
- **R8 — Every remote dependency defines timeout/retry/circuit-breaker.**
- **R9 — Every state machine defines legal transitions; invalid transitions
  are rejected explicitly.**

## Communication topology

| Need | Mechanism | Why |
|------|-----------|-----|
| External client → platform | REST via API Gateway | Standard, evolvable, documented (§25) |
| Synchronous service-to-service request/response where latency-critical & typed | gRPC | Type-safe contracts, streaming, lower overhead than REST+JSON for internal calls (used selectively, e.g. risk evaluation) |
| State changes / cross-service events | Kafka (events) | Decoupling, replay, eventual consistency |
| Atomic "state change + event" | Transactional Outbox | No lost events when DB commits but Kafka is down (ADR-0004) |

## Transaction & consistency model

- **Local ACID** inside a service (PostgreSQL). Invariants such as
  `SUM(debits) == SUM(credits)` are protected by DB constraints where practical
  (§17).
- **Cross-service** = eventual consistency via events + SAGA (ADR-0003).
- **No 2PC / XA** as a default (Rule 2).

## What we deliberately avoid (§52)

- Generic `Repository<T>` everywhere — only where it earns its place.
- CQRS on every entity — only for read-heavy, differently-shaped queries
  (transaction search, audit search, reporting).
- Event choreography where orchestration gives clearer control of a financial
  workflow — Transfer SAGA is **orchestrated** (ADR-0003).

## Hexagonal boundaries, concretely

```
   REST/gRPC/Kafka (interfaces)
            │  ports (interfaces in application/domain)
            ▼
   application use cases  ──► domain model (pure)
            │
            ▼
   infrastructure adapters (JPA, Kafka, REST client) implement the ports
```

A domain service never imports `org.springframework`. If you see
`domain/.../infrastructure` you have a violation.

## Architecture quality gates (§51)

Before a phase closes, the review loop checks: bounded contexts clear,
aggregates protect invariants, transaction boundaries correct, operations
idempotent, Kafka keys/ordering/duplicates handled, authz/secrets/TLS/audit in
place, metrics/logs/traces/health present, K8s readiness/limits/HPA/PDB/
NetworkPolicy/GitOps present.

## Violation handling

Architecture violations are caught by:
- **ArchUnit** tests per service (domain has no framework deps; layering
  respected).
- Manual review (Hermes review loop, §49).
