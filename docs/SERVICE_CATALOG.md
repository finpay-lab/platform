# Service Catalog

Each service is a **bounded context** with its own database. Status reflects the
implementation roadmap (§46). Phases are refined by Hermes, not carved in stone.

| Service | Responsibility | Owns data | Async out | Phase |
|---------|---------------|-----------|-----------|-------|
| **identity-service** | Integration with OIDC IdP; maps external subject → internal principal; roles/permissions; session/security metadata | identity, role/permission grants, session metadata | `UserAuthenticated`, `RoleChanged` | 1 |
| **customer-service** | Customer profile, status (PENDING/ACTIVE/SUSPENDED/BLOCKED/CLOSED), KYC state, preferences | customer, kyc | `CustomerCreated`, `CustomerStatusChanged`, `KycStateChanged` | 1 |
| **account-service** | Financial accounts, lifecycle, currency, ownership, status (OPEN/FROZEN/SUSPENDED/CLOSED) | account | `AccountCreated`, `AccountStatusChanged` | 1 |
| **wallet-service** | Wallet (available/pending balance, currency, state). **Not** the accounting source of truth | wallet | `WalletDebited`, `WalletCredited`, `WalletReserved` | 3 |
| **ledger-service** | **Double-entry immutable ledger.** Postings, debits/credits, reversals. Source of truth for money movement | ledger (postings, entries, balances) | `LedgerEntryPosted`, `LedgerReversed` | 2 |
| **payment-service** | Payment lifecycle state machine (CREATED→PENDING→PROCESSING→COMPLETED/FAILED/CANCELLED/REVERSED); idempotent creation | payment | `PaymentCreated`, `PaymentCompleted`, `PaymentFailed` | 3 |
| **transfer-service** | Money-transfer orchestration (validation→reserve→risk→debit→credit→finalize→notify); **SAGA orchestrator** | transfer (saga state) | `TransferCreated`, `TransferCompleted`, `TransferFailed`, `TransferReversed` | 4 |
| **risk-service** | Simplified risk engine: velocity, amount/limit, country, device, patterns. gRPC for low-latency sync eval | risk rules, customer risk level | `RiskCheckCompleted` | 6 |
| **limit-service** | Daily/transaction/customer/account limits with **concurrency-safe** usage accounting | limits | `LimitExceeded` | 6 |
| **notification-service** | Async EMAIL/SMS/PUSH; failures must not affect financial flows | notification (attempts) | (consumes `NotificationRequested`) | 7 |
| **reconciliation-service** | Compares ledger vs payment/transfer; detects missing/dup/amount/status mismatches, orphans | reconciliation reports | (consumes ledger/payment/transfer events) | 9 |
| **audit-service** | Immutable WHO/WHAT/WHEN/RESOURCE/ACTION/RESULT/CORRELATION_ID records | audit (append-only) | (consumes audit events) | 9 |
| **search-service** | OpenSearch index for transaction/audit/customer search; **not** source of truth | search index (derived) | (consumes domain events) | 8 |
| **gateway** (separate module) | AuthN enforcement, routing, rate limiting, correlation ID, CORS, threat protection. **No business logic** | — (delegates to IdP + services) | — | 1 |

> Status legend: *planned* = not started. Phases are implemented as vertical
> slices; a service may be partially present (e.g. read path before write path)
> within a phase.
