# TASK-009 — Event & API contracts + versioning policy

**CONTEXT**
P0 foundation (TASK-001) is complete and pushed (`0a6d2d1`); build is green.
`EVENT_CATALOG.md` already lists the domain events and topics. We now need the
concrete, versioned contract artifacts that every service and consumer depends
on, plus an explicit versioning policy (§12, §25, §53). This is a contracts-only
task — NO service implementation.

**PROBLEM**
Without checked-in schemas, producers and consumers drift; out-of-order /
duplicate / schema-change scenarios (§13) become unmanageable. A documented
versioning policy prevents breaking changes.

**GOAL**
Create `contracts/events/*` schemas, `contracts/api/*` API stubs, and a
`VERSIONING.md` policy so subsequent service tasks implement against stable
contracts.

**CONSTRAINTS**
- Align field names/types with `EVENT_CATALOG.md` (eventId, eventType,
  occurredAt, version, partitionKey, payload envelope).
- JSON Schema (draft 2020-12) for events; OpenAPI 3.1 for API stubs.
- Each event schema is versioned (`v1/`, `v2/...`); changes are additive +
  backward-compatible unless a new major version is created.
- No business logic; these are static contract artifacts.
- Keep the repo buildable — these files must not break `gradle build` (they are
  not compiled; just ensure nothing under `contracts/` is picked up by Gradle).
- Work ONLY inside the repository (see AGENTS.md sandbox note). Do NOT write to
  `/tmp`.

**ARCHITECTURE**
Envelope (every event):
```
{ "eventId": uuid, "eventType": string, "occurredAt": iso-8601,
  "version": int, "partitionKey": string, "payload": { ... } }
```
Topic naming + partition key per `EVENT_CATALOG.md` (topic-per-domain-stream,
keyed by customerId/accountId/transferId).

**FILES TO TOUCH**
- `contracts/events/v1/CustomerCreated.json`, `AccountCreated.json`,
  `FundsReserved.json`, `FundsReleased.json`, `TransferCreated.json`,
  `TransferCompleted.json`, `TransferFailed.json`, `LedgerEntryPosted.json`,
  `PaymentCompleted.json`, `PaymentFailed.json`, `RiskCheckCompleted.json`,
  `NotificationRequested.json` (JSON Schema, each with a `$id` + example).
- `contracts/api/v1/identity-service.openapi.yaml` (auth/principal endpoints
  stub), `contracts/api/v1/account-service.openapi.yaml` (account + status stub)
  — establish the API contract pattern; do NOT implement the services.
- `contracts/VERSIONING.md` — event + API versioning policy: semantic versioning
  of schemas, backward-compatible evolution rules, deprecation + sunset windows,
  schema-registry intent (lab uses file schemas; prod uses a registry), API URL
  versioning (`/v1`), and how consumers must handle unknown/extra fields
  (tolerate, never fail).

**ACCEPTANCE CRITERIA**
- All 12 event schemas present, valid JSON Schema, with a concrete example.
- API stubs present and valid OpenAPI 3.1.
- `VERSIONING.md` covers event + API versioning, compatibility, deprecation.
- `gradle clean build` still GREEN (contracts are inert to Gradle).
- No service code, no `/tmp` writes.

**TEST REQUIREMENTS**
- Validate the JSON Schemas parse (e.g. a tiny `node` one-liner or Python
  `jsonschema` if available) — report which validator was used. No Gradle test
  needed for static artifacts, but confirm the repo still builds.

**NON-GOALS**
- No Kafka producer/consumer code. No service implementation. No schema-registry
  server.

**VALIDATION COMMANDS**
- `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.10.2-jdk21 gradle clean build` (must stay green)
- JSON schema validation of `contracts/events/**`.

**ENGINEER REPORT FORMAT**
- files changed
- design decisions (envelope, versioning choices)
- validator used + result
- build result
- failures / concerns
