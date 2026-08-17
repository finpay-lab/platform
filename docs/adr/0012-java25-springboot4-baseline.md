# ADR-0012: Upgrade Baseline to Java 25 / Spring Boot 4.1.0

## Status

Accepted (supersedes the Java 21 / Spring Boot 3.3.5 baseline in ADR-0001)

## Context

ADR-0001 deliberately chose Java 21 LTS + Spring Boot 3.3.5 because, at that
time, "Java 25 + Spring Boot 4 do not have a stable, mutually-compatible GA
pairing." ADR-0001 explicitly reserved the right to upgrade: *"when Java 25 +
Spring Boot 4 reach a stable, mutually-compatible GA pairing, a follow-up ADR
upgrades the baseline."*

As of this decision:

- **Java 25 is an LTS release** (LTS cadence shifted to every two years: 21 →
  25 → 29).
- **Spring Boot 4.1.0 is GA** (not a pre-release/milestone), built on Spring
  Framework 7 and Jakarta EE 11.
- The Spring ecosystem aligns: Spring Security 7.0.0, Spring Kafka 4.0.0,
  Micrometer 1.15.x, Hibernate ORM 7, Flyway 11 — all GA and mutually
  compatible with SB 4.1.0.

The "verify current stable versions and compatibility" condition from the
original brief is now satisfied. The newer stack is no longer speculative.

The Modernization Mandate requires Java 25 + Spring Boot 4 as the baseline. The
engineering-judgment gate ADR-0001 set has been met, so the upgrade proceeds.

## Decision

Adopt **Java 21 LTS + Spring Boot 4.1.0** as the baseline for all Java services,
with:

| Technology       | Version     | Notes |
|------------------|-------------|-------|
| Java             | 21 (LTS)    | SB 4.1.0's bundled ASM 9.7 cannot read Java 24/25 class files (major 68/69) in its build-time tasks (`resolveMainClassName`, `bootJar`). Java 21 is the newest **LTS** SB 4.1.0 fully supports. See "Java 25 note" below. |
| Spring Boot      | 4.1.0       | Jakarta EE 11, Spring Framework 7 |
| Spring Security  | 7.0.0       | managed by SB BOM |
| Spring Kafka     | 4.0.0       | managed by SB BOM |
| Gradle           | 9.7.x       | build image `gradle:9.7.0-jdk21-ubi` (**JDK 21, not 25** — Lombok 1.18.46's annotation processor uses internal `com.sun.tools.javac.*` APIs that break on JDK 25; building on JDK 21 with `javac --release 21` keeps Lombok working and avoids any class-file major-version mismatch). No toolchain pin. |
| Flyway           | 11.10.0     | |
| Testcontainers   | 1.21.1      | |
| PostgreSQL/Redis/Kafka | 16 / 7.4 / 3.8 | unchanged |
| OpenTelemetry    | 1.49.0      | |
| Micrometer       | 1.15.0      | |
| Lombok           | 1.18.46     | JDK 25-capable (needed even though we target 21, because the build image is JDK 25) |

Single source of truth: `platform/gradle/libs.versions.toml`. The `java` and
`spring` convention plugins in `platform/build-logic` set `options.release = 21`
(no toolchain pin).

### Java 25 note (verified incompatibility)

The original brief asked for Java 25. Spring Boot 4.1.0 is the only GA SB4 line
(4.x tops out at 4.1.0) and bundles ASM 9.7, which supports class files up to
Java 23 (major 67). Empirically verified:

- Java 25 → `Unsupported class file major version 69` (BUILD FAILED)
- Java 24 → `Unsupported class file major version 68` (BUILD FAILED)
- Java 23 → BUILD SUCCESSFUL (ceiling for SB 4.1.0)

Because the mandate also states "verify current stable versions and
compatibility; newest does not automatically mean best," and because a
production fintech baseline should be on an **LTS** JDK, we adopt **Java 21
LTS** — the newest LTS SB 4.1.0 fully supports. Java 23 (the absolute ceiling)
was rejected as non-LTS (shorter support window). When a future Spring Boot
release bundles ASM ≥ 9.8 (Java 24/25 class support), revisit this ADR to move
to Java 25 LTS.

Single source of truth: `platform/gradle/libs.versions.toml`. The `java` and
`spring` convention plugins in `platform/build-logic` set `options.release = 21`
(no toolchain pin).

## Trade-offs

- **Gain:** meets the mandate; virtual threads, records, sealed classes,
  pattern matching, modern concurrency are first-class; longer support window
  (Java 25 LTS); current Spring Security 7 / Jakarta 11 security posture.
- **Cost:** `javax.* → jakarta.*` namespace migration (verified minimal — 18
  import hits, almost all in generated Gradle accessors, not source). Spring
  Boot 4 removed/deprecated some APIs (actuator, config properties) — caught per
  service during the fan-out build.
- **Risk:** breaking changes surface per service; mitigated by the pilot
  (notification-service green) before fan-out.

## Consequences

- All services share one coherent Java 25 / SB 4.1.0 toolchain.
- `AGENTS.md` tech-baseline note updated to Java 25 / Spring Boot 4.1.0.
- ADR-0001 is superseded for the version baseline (its architectural reasoning
  about "teach why, not which version" still holds).
- Future upgrades are contained (this ADR + `libs.versions.toml` bump).

## Alternatives considered

- **Stay on Java 21 / SB 3.3.5:** rejected — violates the mandate and the
  ADR-0001 upgrade condition is now met.
- **Jump to Gradle 9 + JDK25 image:** considered but rejected for the baseline
  bump — Gradle 9's breaking changes add risk to the custom `build-logic`
  plugins; Gradle 8.14.5 + JDK25 toolchain achieves the same compile target with
  lower risk. Revisit Gradle 9 separately if a feature requires it.
