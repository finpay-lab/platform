# VERSIONING — Event & API contract policy

Concrete, checked-in contracts live in `contracts/`:

- `contracts/events/<major>/<EventName>.json` — JSON Schema (draft 2020-12),
  one schema per domain event (`EVENT_CATALOG.md`).
- `contracts/api/<major>/<service>.openapi.yaml` — OpenAPI 3.1 stubs, one per
  public service API.

This document is the **versioning policy** producers and consumers MUST follow
(§12 events, §25 API, §53 evolution) — the same policy a production schema
registry would encode (see [Registry intent](#schema-registry-intent)).

---

## 1. The event envelope (every message)

Every event is a JSON object with the following envelope; the payload is
event-specific:

```
{
  "eventId":       uuid       # global event id; consumers dedupe on this (Rule 7)
  "eventType":     string     # fixed discriminator, matches the schema name
  "occurredAt":    iso-8601   # UTC, when the event occurred
  "version":       int        # MAJOR schema version of this event (= path segment)
  "partitionKey":  string     # = the Kafka key (EVENT_CATALOG.md)
  "payload":       object     # event-specific, documented inline
}
```

Rationale:

- `eventId` → consumers are idempotent on it (at-least-once + outbox
  redelivery, ADR-0004, §13). Duplicate Events are ignored, never re-applied.
- `partitionKey` → equals the topic's business key so per-key ordering is
  preserved and consumers scale horizontally.
- `eventType` → lets consumers/stream filters discriminate without opening the
  payload, and gives a cheap assertion that the schema matches the stream.
- `version` → lets a consumer safely *ignore* envelopes whose major it does not
  understand, matching "tolerate, never fail".

> Schemas deliberately do **not** set `additionalProperties: false`. Unknown
> fields are permitted by default so that future additive fields never invalidate
> existing consumers (see §4).

## 2. Event schema versioning

Schemas follow **semantic versioning** of the *contract shape*:

- **Major** (raw-version) is encoded in the directory: `v1/`, `v2/`, … and in
  the envelope `version` field (and the schema `$id`). A new major = a new
  directory + a new `$id`. The previous major directory is **never modified or
  deleted** during its sunset window.
- **Minor / patch** (additive, backward-compatible) is **not** a path segment:
  such changes are applied to the file *in place*. History is traceable via git.
  This keeps consumer tooling stable: an additive change does not force a new
  artifact or a consumer rebuild.

### 2.1 Backward-compatible (minor) changes — allowed in place

Allowed, must NOT trigger a major bump or a new directory:

- Adding a new (preferably optional) payload field.
- Widening an `enum` (adding a new legal value) — e.g. a new `kycState`.
- Relaxing a constraint (e.g. `minLength` → shorter, `maxLength` → longer).
- Strengthening a `description` or adding an example.
- Adding a new **event** to an existing stream/topic: `eventType` gains a new
  constant (`EVENT_CATALOG.md`). Consumers that do not recognise the type MUST
  ignore it, not fail.

Forbidden within a major (these are breaking):

- Renaming or removing a field.
- Changing a field's type, changing the semantics of a value.
- Adding a **new required** field to a payload already in production.
- Removing a value from an `enum`.
- Changing the `partitionKey` semantics (the key IS the ordering contract).

### 2.2 Breaking (major) changes — new `vN+1/`

A new major (`v2/`, …) is created ONLY for breaking changes:

1. **Deprecate** the old major (announcement + `deprecated: true` suffix in
   docs / CI report).
2. **Dual-write window** — the producer continues to emit the old major on the
   same topic while the new major is also emitted, so consumers migrate at their
   own pace. `eventType` is preserved; `version` disambiguates.
3. **Sunset** — after the window, the producer stops emitting the old major; the
   `v1/` artifact remains checked in for historical consumers and replay, until
   its consumers decommission it.

Lab minimums (tune per orbit): the dual-write window runs for **at least one
full release/demo cycle** (~2 weeks in this lab), and the deprecated artifact
stays **at least 30 days** after last producer emission.

### 2.3 Compatibility rule for consumers

- Consumers MUST be able to read any *older* major for which they were built,
  and MUST ignore (log + skip) envelopes whose `version` is newer than the
  highest they support.
- Consumers MUST NOT fail on unknown/extra fields — validate only the fields
  they depend on, then persist a **forwarded copy or explicit projection**, not
  a JSON object that round-trips unknown fields blindly.
- Consumers MUST deduplicate on `eventId` and be idempotent on the business key
  (§13, Rule 7), so redelivered envelopes and the dual-write window are harmless.

## 3. API versioning

- **Versioning is explicit and in the URL**: `/v1/...`. No content negotiation,
  no version headers, no version in the body. (Motive §25: evolvable,
  documented, greppable in logs/metrics; a client can see its version at a
  glance.)
- `openapi` `info.version` mirrors the segment (`1.0.0` → `/v1`).
- **Within a major**: additive, backward-compatible changes (new optional field,
  new endpoint, new enum value, response field added) are allowed in place.
  New endpoints are additive.
- **Breaking changes** require a new URL segment `/v2` (new file
  `contracts/api/v2/<service>.openapi.yaml`). Old `/v1` remains active for the
  sunset window (lab minimum: two release cycles), then is retired; the contract
  file is kept for reference.
- Version strings are major-only (`v1`, `v2`) — no `/v1.2` path segments; minor
  granularity is tracked via git history and release notes.

API-breaking changes include: removing/renaming a path or parameter, changing a
request/response field's type, removing a field from a response, changing
status codes or error semantics.

## 4. Unknown / extra fields — "tolerate, never fail"

Applies to BOTH events and API responses:

1. A consumer MUST NOT reject a document for containing fields it does not
   know.
2. A consumer MUST define its own document type **without** `additionalProperties:
   false`, or strip unknown fields into a projection — so newly added fields do
   not cause deserialization failure.
3. Plain-deserialization drift is a **bug** and must be caught by contract tests,
   not production.

## 5. Schema-registry intent

This lab uses **checked-in file schemas** under `contracts/` as the single
source of truth (reviewed via PR, versioned via git). Production replaces the
"file + git" mechanism with a schema registry (e.g. Confluent Schema Registry /
Apicurio) without changing the *policy*:

- Same `$id` per artifact; the registry stores all majors (`v1`, `v2`, …).
- Compatibility mode = **BACKWARD_TRANSITIVE**: any new schema must be readable
  by consumers of all previous schemas.
- Producers publish against the schema; consumers subscribe with *their required
  fields* — the registry rejects incompatible pairing, and Kafka clients embed
  the raw payload for out-of-registry fallback tools.

## 6. Naming, identifiers & money

- Event schemas: `contracts/events/v1/<EventName>.json`; `$id` =
  `https://finpay.example/contracts/events/<major>/<EventName>.json`. `$id` is
  immutable per artifact; a new major gets a new `$id`.
- Service APIs: `contracts/api/v1/<service>.openapi.yaml`.
- **Identifiers** (`*Id`) are UUID strings (`format: uuid`).
- **Money** is a decimal string (`format: decimal`), never a float — no
  floating-point arithmetic in financial paths (Rule 6, §17). Currency codes are
  ISO-4217 `^[A-Z]{3}$`.
- Use-case boundaries: controller/transport ↔ use case only (Rule R3); the
  schema names the *use case* (operationId), not an endpoint verb.

## 7. Change procedure (practical)

1. Author additive changes in place; add/adjust the `examples` array.
2. For breaking changes: create `vN+1/`, deprecate `vN/` in `EVENT_CATALOG.md` /
   service docs, start the dual-write window, schedule the sunset.
3. Validate changed schemas (JSON-Schmea draft-2020-12; locally the one-liner
   shown in the TASK-009 notes) and keep `gradle clean build` green; contracts
   are inert to Gradle but reviewed in PR.
4. Record reasons in the commit message / ADR when a new major is introduced.