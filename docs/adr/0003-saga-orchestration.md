# ADR-0003: SAGA Orchestration (Transfer)

## Status
Accepted

## Context
Money transfer spans customer/account/wallet/ledger/risk/limit/notification.
Distributed transactions (2PC/XA) are rejected (Rule 2). Need clear failure
handling.

## Decision
Use **orchestration** SAGA for Transfer, not choreography.
`transfer-service` is the orchestrator: it persists saga step state and drives
each step, runs explicit **compensation** on failure, and recovers
deterministically after a crash (state in DB, not memory).

## Why orchestration over choreography
- A financial workflow needs explicit, auditable control of ordering and
  compensation.
- Choreography ("each service reacts to events") makes the overall flow and
  failure path hard to reason about and debug for money movement.
- Orchestration gives a single place to observe saga state, fail, and compensate.

## Compensation
Idempotent, keyed by `(transferId, step)`. Reversing ledger entries are new
immutable postings, never edits.

## Why not for everything
Stateless fan-out notifications could be choreography; we keep orchestration
where correctness/compensation matters (money).
