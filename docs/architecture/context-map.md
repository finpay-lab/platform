# Context Map

Bounded contexts and their relationships (DDD, §42). Arrows = dependency
direction of *knowledge*, not runtime calls.

```
                 ┌──────────────┐
                 │  Identity    │  (integrates OIDC IdP)
                 │  Provider    │
                 └──────┬───────┘
                        │ authenticates
   ┌────────────────────┼─────────────────────────────────────────┐
   │                    ▼                                           │
   │              API Gateway                                        │
   │   customer   account   wallet   payment   transfer   risk      │
   │      │         │        │         │         │          │       │
   │      └─────────┴──┬─────┴─────────┴─────────┘          │       │
   │                 ledger (upstream, source of truth)◄──────┘       │
   │                     │                                           │
   │   notification  reconciliation  audit  search (all downstream  │
   │   (consumers)   (consumers)     (sink)  (derived index)        │
   └─────────────────────────────────────────────────────────────────┘
```

## Relationships

- **customer → account, wallet:** a customer owns accounts/wallets (reference by
  id; no shared DB).
- **transfer → (customer, account, wallet, risk, limit, ledger, notification):**
  orchestrator depends on many; others are unaware of transfer. This is the
  pivotal context.
- **ledger → (wallet, reconciliation, search):** ledger is upstream; wallet
  reconciles to it.
- **risk, limit → transfer:** sync (gRPC) decision suppliers.
- **notification, audit, search, reconciliation:** downstream consumers;
  publishing services do not depend on them (fire-and-forget via outbox).

## Anti-corruption

Each service translates external events into its own domain model at the
boundary (no leaking another context's types into the domain layer).

## Conformist vs. separate ways

- `identity-service` is a **conformist** to the IdP's token model (maps claims →
  internal roles).
- `ledger-service` is **separate way** — it defines money semantics
  independently; everyone else conforms to ledger's posting model.
