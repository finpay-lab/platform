# Transaction Flows

Two flows demonstrate the hardest distributed-systems properties. Both are
implemented in later phases; this doc is the contract they must satisfy.

## Flow 1 — Money Transfer (SAGA orchestration, ADR-0003)

Orchestrator: `transfer-service`. All steps are persisted as saga state so a
crash is recoverable.

```
Create Transfer (idempotency key)
   │
   ▼  validate: customer ACTIVE, account OPEN, beneficiary valid
   ▼  check limits (limit-service, sync)
   ▼  risk evaluation (risk-service, gRPC sync)
   ▼  reserve funds (wallet-service: available→pending, reservationId)
   ▼  debit source  (ledger-service: immutable posting, source -amount)
   ▼  credit dest   (ledger-service: immutable posting, dest +amount)
   │      invariant: SUM(debits)==SUM(credits)
   ▼  finalize: release reservation, mark transfer COMPLETED
   ▼  notify (NotificationRequested event → notification-service)
```

### Compensation (failure path)

```
debit succeeds
   │
credit fails (timeout / validation)
   ▼  compensate:
        • post reversing ledger entry (source +amount) → LedgerReversed
        • release reservation (wallet available restored)
        • mark transfer FAILED / REVERSED
        • emit TransferFailed / TransferReversed
        • notify customer (failure noticed)
```

Compensation steps are **idempotent** (keyed by transferId + step) so a crash
during compensation is safe to re-run.

### Crash mid-SAGA (§37)

`reserve` done, service crashes. On restart, the persisted saga state shows
`reserve` completed, `debit` pending → orchestrator **continues** (or, if a
timeout/DLQ indicates abort, runs compensation). Deterministic because state is
in the DB, not in memory.

## Flow 2 — Idempotent Payment (Rule 6, §10)

```
POST /payments   Idempotency-Key: K
   │
   ▼  idempotency store (Redis/DB):
        • key K exists → return stored previous result (200/201)
        • key K absent → store IN_PROGRESS, process
   │
   ▼  create payment (PAYMENTS table, unique(idempotencyKey))
   ▼  outbox: PaymentCreated
   │
   ▼  store COMPLETED result under K, return
```

### Concurrency / edge cases

- **Concurrent identical K:** one wins the unique constraint; the other reads
  the committed result. Never two payments.
- **Different payload, same K:** rejected (409 / conflict) — key binds to the
  first payload's hash.
- **Client retry / server retry:** safe — same K returns same outcome.
- **Expiration:** IN_PROGRESS entries TTL; a stale in-flight is reconciled.

## Flow 3 — Ledger posting (invariant enforcement)

`ledger-service` accepts a posting request (debit account A, credit account B).
Within one DB transaction it writes both entries and asserts
`SUM(debit)==SUM(credit)` via a DB constraint / trigger. Entries are immutable;
corrections are new reversing postings, never updates. Balance = sum of postings
(keyed by account, with optimistic locking on the balance row to prevent
lost-update on concurrent postings — §22 double-spend prevention).
