# ADR-0005: Database-per-Service & Ledger as Money Source of Truth

## Status
Accepted

## Context
Brief mandates database-per-service (Rule 1) and that the wallet must NOT be
the source of truth for accounting.

## Decision
- **Each service owns its PostgreSQL database/schema** + Flyway migrations.
  No shared DB, no cross-service queries (Rule 1).
- **ledger-service is the single source of truth for money movement.** Wallet
  balances are an operational cache for UX/speed, reconciled against ledger. On
  discrepancy, ledger wins.

## Why
- Autonomous deployability and fault isolation per service.
- A single immutable ledger makes double-entry invariants enforceable and
  auditable; a mutable wallet balance alone could drift.

## Consequences
- Cross-service reads happen via events (CQRS read models) or explicit APIs.
- reconciliation-service continuously checks wallet/ledger agreement.

## Rejected
Shared database (coupling, lock contention, blast radius) is forbidden without a
documented ADR reason (none currently).
