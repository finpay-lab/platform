# services/ — business service modules (P1+)

This directory is intentionally **empty in the foundation phase** (TASK-001).

Per `PROJECT_TASKS.md`, business services land in later phases and are
included from the root `settings.gradle` under the `services:` group, e.g.:

```
include 'services:identity-service'
include 'services:customer-service'
include 'services:account-service'
include 'services:gateway'
include 'services:ledger-service'
include 'services:payment-service'
include 'services:transfer-service'
include 'services:risk-service'
include 'services:limit-service'
include 'services:notification-service'
include 'services:search-service'
include 'services:reconciliation-service'
include 'services:audit-service'
```

Each service:

- applies the `com.finpay.*` convention plugins from `build-logic/`
  (java-library baseline from `com.finpay.spring-conventions`, with
  `bootJar { enabled = true }`);
- owns its database schema + Flyway migrations (no shared DB, ADR-0005);
- keeps domain logic free of infrastructure imports (Architecture Rule 4) —
  enforced by the shared `ArchitectureRules` checks in `libraries/common-test`;
- defines idempotency, outbox, and event-contract integrations against the
  conventions established by the `libraries/*` shared modules.

Nothing here yet by design: the foundation must first stay buildable
(`gradle build` green) with zero business logic.