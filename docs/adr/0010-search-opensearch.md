# ADR-0010: Search via OpenSearch (CQRS read side)

## Status
Accepted

## Context
Need transaction/audit/customer search and operational investigation. A
relational DB is poor at free-text/aggregated search across services.

## Decision
Use **OpenSearch** as a **derived** search index (CQRS query side). PostgreSQL
remains source of truth; domain events flow via Kafka → search indexer →
OpenSearch. Index is **not** source of truth; rebuildable from events.

## Why not everywhere
Only transaction search, audit search, and customer search justify it. We do
**not** introduce search "because fintech uses it" (brief's anti-cargo-cult).

## Handling
- Reindexing via event replay / snapshot.
- Duplicate events tolerated (idempotent upsert by id).
- Mapping evolution via index aliases + versioned mappings.
- Missing events: reconcile from source on a schedule.
- Index unavailable → financial transactions unaffected (search is best-effort).
