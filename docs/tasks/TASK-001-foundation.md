# TASK-001 — Repository skeleton + Gradle multi-module foundation

**CONTEXT**
`microservice-lab` is a fintech distributed-systems laboratory (FinPay). The
First Hermes Deliverable — architecture docs (SYSTEM_OVERVIEW, ARCHITECTURE,
SERVICE_CATALOG, DATA_OWNERSHIP, EVENT_CATALOG, SECURITY, OBSERVABILITY,
LOCAL_DEVELOPMENT, adr/*, architecture/*, PROJECT_PLAN, PROJECT_TASKS) — is
complete. We now build the executable foundation (P0) so every later service
inherits consistent engineering conventions and a buildable multi-module Gradle
project. No business logic in this task.

**PROBLEM**
Without a shared build foundation, each service would reinvent build config,
dependency versions, and conventions, leading to drift and a non-reproducible
lab.

**GOAL**
Create the Gradle multi-module skeleton + a `build-logic` precompiled-convention
plugin set + the directory layout that later services plug into. Prove it
builds.

**CONSTRAINTS**
- Java 21 LTS, Spring Boot 3.3.x, Gradle 8.10.x (ADR-0001).
- Multi-module: root `settings.gradle` includes `build-logic` (convention
  plugins) and a `libraries/` group (shared libs) and `services/` group (empty
  placeholder for now — do NOT implement business services).
- Architecture Rule 4: domain-independent conventions only.
- Repository must remain continuously buildable (`gradle build` green).

**ARCHITECTURE**
- `build-logic/` = precompiled Groovy convention plugins:
  `com.finpay.java-conventions`, `com.finpay.spring-conventions`,
  `com.finpay.testing-conventions`, `com.finpay.quality-conventions`.
- `gradle/libs.versions.toml` = centralized versions (Spring Boot BOM, Spring
  Security, Spring Data JPA, Spring Kafka, Flyway, PostgreSQL, Redis,
  OpenTelemetry, ArchUnit, Testcontainers, JUnit5, AssertJ, Mockito).
- `settings.gradle`: `pluginManagement` + `dependencyResolutionManagement`
  (version catalog) + include `build-logic` and module stubs.

**FILES TO TOUCH**
- `settings.gradle`, `build.gradle` (root), `gradle.properties`,
  `gradle/wrapper/gradle-wrapper.properties`, `gradle/libs.versions.toml`
- `build-logic/build.gradle`, `build-logic/settings.gradle`,
  `build-logic/src/main/groovy/com.finpay.*.gradle`
- `.gitignore`, `README.md`, `.github/workflows/ci.yml` (skeleton)
- `docker-compose.yml` (local infra: postgres, kafka(KRaft), redis, opensearch,
  keycloak, prometheus, grafana, otel-collector)
- `services/README.md` (note: services land in later phases)

**ACCEPTANCE CRITERIA**
- `gradle projects` lists root, build-logic, and the libraries/services groups.
- A trivial `libraries/common-test` (or `common-web`) module applies the
  conventions and compiles + has one passing test (proves the plugin chain).
- `gradle build` is GREEN.
- `docker compose config` validates (infra defined, not necessarily running).
- No business/domain code.

**TEST REQUIREMENTS**
- At least one module builds + one unit test passes (e.g. a trivial
  `ArchitectureRules` smoke test or a constant test).

**NON-GOALS**
- No identity/customer/account/ledger/payment/transfer/etc. logic.
- No Kubernetes/Helm/Terraform/Ansible yet (later phases).
- No real microservice implementations.

**VALIDATION COMMANDS**
- `./gradlew projects`
- `./gradlew build`  (or via Docker Gradle image: `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.10.2-jdk21 gradle build`)
- `docker compose config`

**ENGINEER REPORT** (OpenCode, 2026-08-12)
- Files changed: settings.gradle, build.gradle, gradle.properties, gradle-wrapper,
  gradle/libs.versions.toml; build-logic/ (4 flat convention plugins); libraries/
  common-{web,security,observability,test} (each build.gradle + class + test);
  docker-compose.yml, .gitignore, README.md, .github/workflows/ci.yml,
  observability/{prometheus,prometheus.yml,otel/otel-collector.yaml},
  services/README.md.
- Design decisions: precompiled Groovy convention plugins kept FLAT (spring-
  conventions applies java-library + spring-boot + dependency-management, never
  applies sibling conventions by id); libraries set bootJar.enabled=false; used
  libs.* catalog accessors inside plugins (raw strings fail to resolve).
- Tests: ErrorCodeTest, RoleTest, TraceContextTest, ArchitectureRulesTest — all
  PASS. `gradle clean build` → BUILD SUCCESSFUL (architect re-verified).
- Concerns:
  1. `opentelemetry-extension-annotations:1.43.0-alpha` unavailable on Maven
     Central → removed from catalog/module; real OTel annotation artifact added
     deliberately in observability phase (TASK-005+).
  2. Gradle 8→9 deprecation warning eliminated by declaring junit-platform-launcher
     in testing-conventions; final build shows none.
  3. ArchitectureRules.domainIsIndependentOfInfrastructure() is defined but NOT
     yet executed as a check-time gate (its test only asserts the rule object
     exists). Wire rule.check(...) into service test suites in later tasks.
  4. No initial commit yet — architect to commit + push.

**REVIEW (Hermes, Architect) — verdict: ACCEPTED (minor follow-ups)**
- Independently re-ran `gradle clean build` → BUILD SUCCESSFUL. 4 libs compile+test.
- Convention plugins verified FLAT (correct per AGENTS.md constraint).
- Rule 4 rule object exists; confirm follow-up #3 lands when first service tests.
- No business logic, no shared DB, no framework leakage in domain/. Foundation
  scope respected. Proceed to TASK-009 (contracts) then P1 services.
