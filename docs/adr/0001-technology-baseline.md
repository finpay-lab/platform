# ADR-0001: Technology Baseline — Java 25 / Spring Boot 4.x vs LTS

## Status

Accepted

## Context

The brief (§40) suggests Java 25 + Spring Boot 4.x but explicitly warns: *"Before
implementation, verify current stable versions and compatibility. Do not blindly
assume version compatibility."*

As of the decision date, the coherent, GA-compatible stack for an enterprise
Java microservice lab is:

| Technology            | Version          | Notes |
|-----------------------|------------------|-------|
| Java                  | 21 LTS            | Java 21 is the current widely-adopted LTS with full Spring Boot 3.x support. Java 25 is not yet a generally-available LTS paired with a stable Spring Boot GA line. |
| Spring Boot           | 3.3.x            | Stable, long-lived, fully compatible with Spring Security 6, Spring Kafka, Spring Data JPA. Spring Boot 4.x is not GA on a stable cadence aligned with Java 21. |
| Gradle               | 8.10.x           | Stable wrapper, good Kotlin DSL, build cache. |
| PostgreSQL            | 16               | Current stable major. |
| Redis                 | 7.4              | Stable. |
| Apache Kafka          | 3.8              | Stable;KRaft mode available (no ZooKeeper). |
| OpenSearch            | 2.17             | Stable search engine. |
| Flyway                | 10.x             | Migration tool. |
| Testcontainers        | 1.20.x           | Integration tests against real infra. |
| OpenTelemetry         | 1.4x (Java SDK)  | Tracing/metrics. |
| Kubernetes            | 1.31             | Stable. |
| Helm                  | 5.x              | Chart packaging. |
| Argo CD               | 2.12             | GitOps. |
| Terraform             | 1.9              | IaC. |
| Ansible               | 10               | Config management. |

## Decision

Adopt **Java 21 LTS + Spring Boot 3.3.x** as the baseline for all Java services.

Rationale: a laboratory exists to teach *why* architecture decisions exist. The
teaching value (SAGA, Outbox, idempotency, CQRS, DDD) is entirely independent of
the exact Java/Spring major. Using a coherent GA stack means every example
compiles, runs, and is testable — which is the prerequisite for demonstrating
the hard distributed-systems scenarios (§37, §53). A speculative Java 25 / Spring
Boot 4 stack that does not have a stable, mutually-compatible GA release would
undermine the "continuously buildable" rule (§58) and teach nothing extra.

This ADR is explicitly revisitable: when Java 25 + Spring Boot 4 reach a stable,
mutually-compatible GA pairing, a follow-up ADR upgrades the baseline. The
intent of the brief (modern, current stable) is satisfied by choosing the
*current stable LTS*, not the newest pre-release.

## Consequences

- All services share one coherent toolchain → reproducible builds, shared Gradle
  convention plugins (build-logic).
- Upgrading later is a contained change (ADR + Gradle version bump + CI matrix).
- We forgo "newest possible version" in favor of "provably works".

## Alternatives considered

- **Java 25 + Spring Boot 4 (as literally written):** rejected — no stable,
  mutually-compatible GA pairing at decision time; violates the brief's own
  "verify compatibility" rule and the buildability rule.
- **Java 17 LTS + Spring Boot 3.2:** viable but older; 21 is the superior
  current LTS and costs nothing extra here.
