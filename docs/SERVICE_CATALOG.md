# Service Catalog

Each service is a **bounded context** with its own database. Status reflects the
implementation roadmap (§46). Phases are refined by Hermes, not carved in stone.

| Service | Responsibility | Owns data | Async out | Phase |
|---------|---------------|-----------|-----------|-------|
| **identity-service** | Integration with OIDC IdP; maps external subject → internal principal; roles/permissions; session/security metadata | identity, role/permission grants, session metadata | `UserAuthenticated`, `RoleChanged` | 1 |
| **customer-service** | Customer profile, status (PENDING/ACTIVE/SUSPENDED/BLOCKED/CLOSED), KYC state, preferences | customer, kyc | `CustomerCreated`, `CustomerStatusChanged`, `KycStateChanged` | 1 |
| **ledger-service** | **Double-entry immutable ledger.** Postings, debits/credits, reversals. Source of truth for money movement | ledger (postings, entries, balances) | `LedgerEntryPosted`, `LedgerReversed` | 2 |
| **transfer-service** | Money-transfer orchestration (validation→reserve→debit→credit→finalize→notify); **SAGA orchestrator** | transfer (saga state) | `TransferCreated`, `TransferCompleted`, `TransferFailed`, `TransferReversed` | 4 |
| **notification-service** | Async EMAIL/SMS/PUSH; failures must not affect financial flows | notification (attempts) | (consumes `NotificationRequested`) | 7 |
| **gateway** (separate module) | AuthN enforcement, routing, rate limiting, correlation ID, CORS, threat protection. **No business logic** | — (delegates to IdP + services) | — | 1 |

> **Scope reduction (2026-08-13).** The lab was trimmed to **6 learning
> services**, one per distinct microservice *case*, to avoid redundant
> bounded contexts that teach nothing new. **Archived (read-only, restorable):**
> `account-service` (overlapped `customer-service`; had real code — archived,
> not deleted), `wallet-service` (derived balances; "not source of truth" per
> lab, redundant with `ledger-service`), `payment-service` (overlapped
> `transfer-service` saga/outbox), `risk-service` + `limit-service` (overlapped
> each other — sync checks in a flow), `audit-service` + `search-service` +
> `reconciliation-service` (generic event consumers, redundant with
> `notification-service`). Each kept service demonstrates a unique pattern:
> gateway (edge), identity (OIDC), customer (baseline CRUD + state machine),
> ledger (hard domain modeling), transfer (SAGA — the distributed-transaction
> lesson), notification (async event consumer + failure isolation).

> Status legend: *planned* = not started. Phases are implemented as vertical
> slices; a service may be partially present (e.g. read path before write path)
> within a phase.
