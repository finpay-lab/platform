# Observability

Every request is observable end-to-end (§27). Three pillars: **metrics**,
**logs**, **traces** — unified by `traceId` / `spanId` / `correlationId`.

## Tracing (OpenTelemetry)

- All services run the **OTel Java agent** (or SDK) → export to **OTel
  Collector** → tempo/trace backend.
- Each inbound request creates a `traceId`; Kafka messages propagate the
  `traceparent` header so a saga spans multiple services and brokers.
- Spans: HTTP server, DB queries, Kafka produce/consume, outbound gRPC/REST.

## Metrics (Prometheus)

- Scrape `/actuator/prometheus` per service.
- **Required business + infra metrics** (non-exhaustive):
  - `payment_success_total`, `payment_failure_total`
  - `transfer_latency_seconds` (histogram)
  - `kafka_consumer_lag`
  - `outbox_pending_events` (health of outbox publisher)
  - `saga_failures_total`, `saga_compensations_total`
  - `idempotency_conflicts_total`
  - `database_connection_pool_usage`
  - `redis_rate_limit_rejected_total`
  - `circuit_breaker_open_total`
- RED method (Rate, Errors, Duration) per endpoint; USE for resources.

## Logging (centralized)

- Structured JSON logs with `traceId`, `correlationId`, `service`, `level`.
- Shipped to a central store (Loki/ELK in lab). **No secrets/PAN/credentials in
  logs.**
- Log levels meaningful: audit-worthy actions at INFO+; never log raw money
  payloads without redaction policy.

## Dashboards (Grafana)

- Per-service health, payment/transfer funnels, Kafka lag, outbox backlog,
  SAGA failure rate, idempotency conflicts, circuit-breaker state.
- SLO panels: p95 latency, error budget.

## Health & readiness

- `startupProbe`, `livenessProbe`, `readinessProbe` distinct (§30):
  - liveness = process alive (restart on fail)
  - readiness = can serve (DB/cache reachable; do **not** include Kafka in
    readiness if the service can still serve reads).
- Outbox publisher exposes pending-event gauge for alerting.

## Failure visibility (the whole point)

The scenarios in §37/§53 are only *demonstrable* because of observability:
- Kafka down → outbox backlog metric climbs → alert.
- Consumer crash → lag climbs → DLQ grows → alert.
- Duplicate payment → idempotency_conflicts metric.
- SAGA partial failure → saga_failures + compensation traces.

See `observability/` for Prometheus rules, Grafana dashboards, OTel collector
config.
