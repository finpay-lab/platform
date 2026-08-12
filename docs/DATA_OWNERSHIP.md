# Data Ownership

**Rule 1 (§41): No shared database.** Each service is the sole owner and writer
of its schema. Other services read only via events or explicit APIs — never by
querying another service's tables.

## Ownership matrix

| Data | Owner | Readers (read-only, async) | Notes |
|------|-------|----------------------------|-------|
| Identity / roles / permissions | identity-service | gateway (authZ), all services (authZ checks) | IdP is external; identity-service caches mappings |
| Customer profile / KYC / status | customer-service | transfer (validate), risk, notification | |
| Account / ownership / status | account-service | transfer (validate source/dest), ledger (account ref) | Account is a *reference*, not money |
| Wallet balances | wallet-service | transfer (reserve/release), reads ledger for truth | Wallet is operational convenience; **ledger is source of truth for money** |
| **Ledger postings & balances** | **ledger-service** | reconciliation, search, reporting | **Single source of truth for money movement** |
| Payment records | payment-service | reconciliation, search, notification | |
| Transfer / SAGA state | transfer-service | notification, reconciliation, search | |
| Risk rules / levels | risk-service | transfer (sync eval via gRPC) | |
| Limits / usage | limit-service | transfer (sync check) | |
| Notification attempts | notification-service | (none; outcome via event) | |
| Reconciliation reports | reconciliation-service | audit, ops | |
| Audit records | audit-service | search, ops | append-only |
| Search index | search-service | UI/ops queries | **Derived**, rebuilt from events |

## The money-source-of-truth rule

`wallet-service` holds *available/pending* balances for fast reads and UX, but
**the ledger is the authoritative record of money**. On any discrepancy, ledger
wins. Wallet balances are reconciled against ledger postings (reconciliation
service). This is documented as ADR-0005.

## Shared database — forbidden

A single shared schema used by multiple services is **not allowed** unless a
later ADR documents a concrete, unavoidable reason (none currently exist).

## Migration ownership

Each service owns its Flyway migrations in its own module
(`src/main/resources/db/migration`). No cross-service migration scripts.

## Consistency summary

- **Inside a service:** ACID (PostgreSQL transaction).
- **Across services:** eventual, via outbox-published events + idempotent,
  duplicate-tolerant consumers.
