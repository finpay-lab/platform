# FinPay Lab — Technology Radar (Phase 1)

Classification per the mandate: **ADOPT / TRIAL / ASSESS / HOLD**. Every entry
includes the mandate's required checks — current stable version, production
maturity, ecosystem, maintenance, security, compatibility, migration cost,
operational complexity. "Newest ≠ best" is enforced below: items not yet GA or
not compatible are deliberately held or trialed, not adopted blindly.

> Verification note: version/GA facts must be re-confirmed at adoption time
> (Spring Boot 4 and some 2026 releases move fast). This radar records the
> assessment as of the audit; CI should pin exact versions in
> `gradle/libs.versions.toml`.

## ADOPT — safe to use now as standard

| Tech | Stable | Why adopt | Caveats |
|------|--------|-----------|---------|
| **Java 25 (LTS)** | 25 (LTS cadence 21→25→29) | Mandate baseline. Virtual threads, scoped values, structured concurrency,Stream gatherers, `Math` strictness, improved FFI. Use when it improves correctness/perf, not for demo. | Requires build toolchain + SB4 alignment. |
| **Spring Boot 4** *(conditional — see TRIAL)* | 4.x GA | Mandate. Spring Framework 7, Jakarta EE 11, native-friendly. | Adopt only once GA + Spring Kafka/Data/Security 4.x align. Until then ADOPT **SB 3.4.x** as interim (supports Java 21–24). |
| **PostgreSQL 16/17** | 17 | Source of truth for money. Strong typing, JSONB, partitioning, logical replication. | 17 needs Flyway 11; test upgrade path. |
| **Kafka (KRaft)** | 3.9 | Event backbone, outbox, DLQ. No Zookeeper. | Client upgrade to match Spring Kafka. |
| **Flyway** | 11 (w/ PG17) / 10 (w/ PG16) | Schema migrations, per-service. | Keep migrations backward-safe. |
| **Redis 7.4** | 7.4 | Idempotency keys, rate-limit, cache, distributed lock. **Not** ledger. | Add fallback to DB unique constraints on outage. |
| **OpenTelemetry** | current | Logs/metrics/traces foundation; Kafka trace propagation. | Instrument once, standardize span attrs. |
| **Testcontainers** | 1.2x | Real PG/Kafka/Redis in integration tests. | Bump from 1.20; Docker-dependent. |
| **Docker / docker-compose** | current | Local infra + image build. | K8s later, not instead. |
| **GitHub Actions** | n/a | CI pipeline (compile→test→scan→build→deploy). | Self-hosted runners if needed. |

## TRIAL — proven, pilot on one service before broadening

| Tech | Why trial | Pilot plan |
|------|-----------|------------|
| **Virtual Threads for JDBC** | Huge throughput win, but JDBC is blocking; pool sizing changes. | Pilot on `ledger-service` read path; measure DB pool saturation; use `spring.threads.virtual.enabled=true` + verify driver blocking behavior. |
| **Spring Boot 4 migration** | Major version; Jakarta EE 11, jakarta.* namespace, removed APIs. | Migrate ONE service (e.g. `notification-service`, smallest real one) first; fix `javax→jakarta`, config props, actuator changes; gate on green build+tests before fan-out. |
| **Spring Security 7 (OAuth2/OIDC)** | RBAC, resource-server JWT, mTLS. | Trial in `gateway` + `identity-service`; enforce authZ at service edge. |
| **gRPC (internal sync)** | Low-latency service-to-service (risk eval, limit check). | Trial `risk`↔`transfer` sync call; REST stays external. Evaluate vs. Kafka. |
| **k6 / Gatling** | Perf measurement (mandate: never claim perf without numbers). | Pilot load test on transfer SAGA; capture p95/p99/error rate. |

## ASSESS — investigate fit, not committed

| Tech | What to assess | Open question |
|------|----------------|---------------|
| **OpenSearch 3.x** | Search read-model (CQRS). Current 2.17 is aging. | Compatibility with Spring Data ES; operational cost vs. benefit for a lab. |
| **Spring Cloud (Gateway/Config/Service-Registry)** | Mandate says *only when justified*. | Do we need service discovery/central config, or is K8s Service + ConfigMap enough? Likely HOLD for lab. |
| **Kubernetes** | Mandate: only if operational benefit justifies complexity. | Justified once >6 deployables + HPA/PDB needed. Defer to P7. |
| **Helm + Argo CD (GitOps)** | Deploy/reconcile. | Assess after K8s decision. |
| **Terraform + Ansible** | IaC for envs + VM bootstrap. | Assess for P9; cloud account (AWS) needed. |
| **AI-assisted engineering (in-loop)** | Coding/test/doc/review assist. | Mandate: AI must NEVER be authoritative for balances/ledger/authZ. Keep financial logic deterministic. |
| **Password hashing (Argon2id)** | Replace any BCrypt. | Adopt when auth-local users exist (currently OIDC-only). |

## HOLD — do not adopt now

| Tech | Why hold |
|------|----------|
| **Distributed transactions (2PC/XA)** | Explicitly rejected (ADR-0003). Use SAGA+Outbox. |
| **Spring Cloud Netflix OSS (Eureka/Ribbon/Hystrix)** | Legacy; replaced by K8s + Resilience4j + Kafka. |
| **Redis as ledger / source of truth** | Violates money-correctness (ADR-0006). Cache/idempotency only. |
| **Floating-point money** | Never. `BigDecimal` / `Money` type only. |
| **Unstable Spring Boot 4 milestones** | Hold until GA; use SB 3.4.x interim. |
| **Service mesh (Istio/Linkerd)** | Over-engineering for lab scale; mTLS via K8s/Spring Security suffices. |

## Movement rules (from mandate)

- Each ADOPT/TRIAL entry gets an ADR before broadening (see `MODERNIZATION_PLAN.md`).
- Re-review the radar at the end of every phase; promote TRIAL→ADOPT only after
  a green pilot with measurements.
- Java 25 and Spring Boot 4 are the baseline; everything else is justified by a
  real problem, not collected for show.
