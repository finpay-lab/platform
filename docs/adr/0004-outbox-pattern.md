# ADR-0004: Transactional Outbox Pattern

## Status
Accepted

## Context
We must publish a domain event after a DB state change. Naive "commit then
publish" loses events if Kafka is down between commit and publish (§37: "DB
commit succeeds, Kafka publish fails").

## Decision
Use the **transactional outbox**: within the same DB transaction as the
business change, also insert an outbox row (event payload + key + version). A
separate **Outbox Publisher** polls unpublished rows and publishes to Kafka
(at-least-once), then marks them published.

## Why
- **No lost events**: if publish fails, the row stays; publisher retries.
- Keeps DB and event emission atomic (single transaction).
- Decouples publish timing from request path (async worker).

## Consumer side
Consumers are idempotent (dedupe by `eventId`) so redelivery after recovery is
safe. This is the pairing that makes at-least-once delivery correct.

## Failure mode
Publisher down → backlog grows (metric `outbox_pending_events` alerts). Kafka
returns → drains. No double state because consumer idempotent.

## Operational cost
One extra table per service + a publisher process/thread; index on
published flag + created_at.
