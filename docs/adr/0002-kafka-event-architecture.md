# ADR-0002: Kafka Event Architecture

## Status
Accepted

## Context
Cross-service state propagation needs decoupling, replay, and eventual
consistency. The brief warns against `topic-per-event`.

## Decision
- **Topic-per-domain-stream**, not per-event (see `EVENT_CATALOG.md`).
- **Partition key = stable business key** (`customerId`, `accountId`,
  `transferId`) for ordering + parallelism.
- **At-least-once** delivery; **consumers idempotent** on `eventId`.
- **DLQ + retry topics** for poison/transient failures.
- **Schema evolution** with backward-compatible field additions; AVRO/JSON
  schemas in `contracts/events/`, versioned.

## Why not simpler
A shared DB or synchronous REST fan-out would couple services and break on
partial failure. Kafka gives async decoupling + replay for search/recon/audit.

## Failure mode
Broker down → outbox backlog (survivable). Consumer down → lag (resumable).
Duplicate delivery → handled by idempotent consumers.

## Operational cost
Running/operating Kafka (KRaft) + monitoring lag + schema registry.
