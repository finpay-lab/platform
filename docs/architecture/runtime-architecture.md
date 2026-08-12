# Runtime Architecture

What happens at runtime for a single external request and for an async event.

## Synchronous external request (REST)

```
Client
  │  Authorization: Bearer <JWT>
  ▼
WAF  (filter malicious)
  ▼
API Gateway
  │  • validate JWT (JWKS)
  │  • rate limit (Redis token bucket)
  │  │ correlate: set correlationId/traceId, propagate
  │  • route, CORS, security headers
  ▼
Microservice (e.g. payment-service)
  │  • RBAC check (scopes)
  │  • controller → application use case (no business logic in controller)
  │  • domain model enforces invariants
  │  • persistence in @Transactional (NO remote calls inside)
  │  • response (consistent error model: code/message/traceId)
  ▼
Client
```

## Asynchronous event (outbox → Kafka)

```
Microservice writes, in ONE DB transaction:
  1. business state change (e.g. payment COMPLETED)
  2. outbox row (event payload + key + version)
COMMIT
  │
Outbox Publisher (separate worker/thread, polls outbox)
  │  • publishes to Kafka (at-least-once)
  │  • marks outbox row published (or deletes)
  ▼
Kafka topic (partitioned by business key)
  │
Consumer (e.g. notification-service)
  │  • dedupe by eventId (idempotent)
  │  • handle duplicate / out-of-order / poison (→ DLQ)
  ▼
Side effect (email) — failure here NEVER rolls back the financial tx
```

This is why "DB commit, Kafka publish fails" (§37) is survivable: the outbox row
remains unpublished; the publisher retries until Kafka accepts it. Consumers
are idempotent so redelivery is safe.

## Service-to-service sync (gRPC)

`transfer-service → risk-service` for a low-latency decision uses gRPC over
mTLS. The call has explicit **timeout + circuit breaker + retry (idempotent
only)**. The decision is recorded; if risk is unavailable, the saga takes the
safe path (e.g. hold for manual review) rather than blindly proceeding.

## Failure posture at runtime

| Failure | Behavior |
|---------|----------|
| Kafka down | outbox backlog grows; metric alerts; no data loss |
| Consumer crash | lag grows; restart resumes from last committed offset |
| Notification failure | financial tx unaffected; retry/DLQ on notify side |
| Search down | financial tx unaffected; index catches up later |
| Redis down | idempotency falls back to DB; rate-limit may fail-open (documented) |
| Service crash mid-SAGA | saga state persisted; orchestrator recovers and continues/compensates deterministically |
