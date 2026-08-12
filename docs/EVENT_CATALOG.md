# Event Catalog

Kafka is the backbone for cross-service state propagation (§12). This catalog is
the contract. Schemas live in `contracts/events/` as versioned JSON/AVRO and are
evolved, never broken (§12, ADR-0002).

## Topic design (intentional boundaries, not topic-per-event)

We do **not** use one topic per event (the brief warns against this). Topics are
grouped by **domain stream**, partitioned by a stable business key for ordering
and consumer parallelism.

| Topic | Producers | Key | Consumers | Purpose |
|-------|-----------|-----|-----------|---------|
| `finpay.customer` | customer-service | `customerId` | wallet, transfer, risk, search, audit | customer lifecycle |
| `finpay.account` | account-service | `accountId` | transfer, ledger, search | account lifecycle |
| `finpay.wallet` | wallet-service | `walletId` | transfer, search | wallet balance changes |
| `finpay.ledger` | ledger-service | `accountId` | reconciliation, search, wallet (recon) | immutable postings |
| `finpay.payment` | payment-service | `paymentId` | notification, reconciliation, search | payment lifecycle |
| `finpay.transfer` | transfer-service | `transferId` | notification, reconciliation, search, risk | transfer lifecycle + saga steps |
| `finpay.risk` | risk-service | `customerId` | transfer | risk decisions |
| `finpay.limit` | limit-service | `customerId` | transfer | limit decisions |
| `finpay.notification` | *many* (via NotificationRequested) | `notificationId` | notification-service | outbound requests |
| `finpay.audit` | audit-service sink | `auditId` | audit-service | immutable audit stream |
| `*.retry` / `*.dlq` | consumer groups | same as source | ops | redelivery / dead-letter |

## Event contracts (subset; full schemas in `contracts/events/`)

| Event | Producer | Key fields | Version |
|-------|----------|-----------|---------|
| `CustomerCreated` | customer-service | customerId, status, kycState, ts | v1 |
| `AccountCreated` | account-service | accountId, customerId, currency, status | v1 |
| `FundsReserved` / `FundsReleased` | wallet-service | walletId, amount, currency, reservationId | v1 |
| `LedgerEntryPosted` | ledger-service | postingId, accountId, debit, credit, amount, currency, ts | v1 |
| `LedgerReversed` | ledger-service | originalPostingId, reversalPostingId, reason | v1 |
| `PaymentCreated` / `PaymentCompleted` / `PaymentFailed` | payment-service | paymentId, amount, currency, status, idempotencyKey | v1 |
| `TransferCreated` / `TransferCompleted` / `TransferFailed` / `TransferReversed` | transfer-service | transferId, from, to, amount, status, sagaStep | v1 |
| `RiskCheckCompleted` | risk-service | customerId, decision, score, rulesHit | v1 |
| `LimitExceeded` | limit-service | customerId, limitType, used, max | v1 |
| `NotificationRequested` | *many* | notificationId, channel, payloadRef | v1 |

## Delivery & consumer semantics (§13)

- **Delivery:** at-least-once. Outbox guarantees the event is eventually
  published; consumers MUST be idempotent on the event's business key.
- **Duplicates:** consumers dedupe by `eventId` (stored in DB/Redis).
- **Out-of-order:** e.g. `TransferCompleted` before `TransferCreated` — consumers
  must buffer or tolerate missing predecessors (idempotent upsert / state
  machine ignores illegal transitions).
- **Ordering:** per-key partition ordering is guaranteed; cross-key ordering is
  not and must not be assumed.
- **Poison messages:** routed to `.dlq` after N retries; never block the
  partition.
- **Schema evolution:** add fields with defaults; do not rename/remove required
  fields without a new major version + dual-write window.

## SAGA event flow (orchestrated, ADR-0003)

`transfer-service` is the orchestrator. It emits `TransferCreated`, then drives
steps via commands and records `TransferCompleted`/`TransferFailed`. On failure
it runs compensation (release reservation, post reversing ledger entries) and
emits `TransferReversed`. Compensation steps are themselves idempotent.
