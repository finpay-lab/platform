## Summary

Adds the shared `libraries:common-ai` module (AI-8): a hexagonal BYOK AI core
that AI-1..AI-7 services will consume. It provides a domain-port `AIClient`
interface plus an OpenAI-compatible HTTP transport, an env/secret key provider
that never hardcodes or logs the key, retry + circuit breaker (Rule 8/9), and
structured audit logging of every AI call (SECURITY.md).

## Changes

- `libraries/common-ai/build.gradle` — new library module (spring-conventions,
  testing-conventions, publish-conventions; `bootJar` disabled by convention).
  Depends on `spring-boot-starter-web` (RestClient) and `spring-test` (tests).
- `settings.gradle` — registers `libraries:common-ai`.
- `gradle/libs.versions.toml` — adds `spring-test` (BOM-managed version).
- Domain/ports (`com.finpay.common.ai`, pure Java, Spring-free):
  - `AIClient` port interface, `AiRequest`/`AiResponse` records, `AiStatus`.
  - `AiException` hierarchy with stable error codes + retryability:
    timeout, rate-limit, service, authentication, circuit-open.
  - `ApiKeyProvider` port, `SecretRedactor` (masked key, never raw),
    `AiAuditEvent` + `AiAuditLogger` port.
- Resilience (`com.finpay.common.ai.resilience`, pure Java):
  - `RetryPolicy` (max attempts, capped exponential backoff, retryable-only).
  - `CircuitBreaker` state machine with enforced legal transitions
    CLOSED -> OPEN -> HALF_OPEN -> CLOSED/OPEN (Rule 9).
  - `ResilientAiClient` decorator combining breaker + retry + exactly one
    audit event per logical call; raw key never reaches audit.
- Infrastructure (`com.finpay.common.ai.infrastructure`):
  - `EnvApiKeyProvider` reads `FINPAY_AI_API_KEY` (env/K8s secret) at call
    time; errors name only the variable.
  - `Slf4jAiAuditLogger` emits structured `audit.ai.call` lines (masked key).
  - `HttpAiClient` OpenAI-compatible REST transport with connect/read
    timeouts; maps 401/403 -> auth, 429 -> rate limit, 5xx -> service,
    transport failures -> timeout. Accepts a pre-built `RestClient` for
    testability.
- Tests (24, all passing):
  - key-not-logged: `SecretRedactorTest`, `Slf4jAiAuditLoggerTest`
    (Logback capture asserts raw key absent), `ResilientAiClientTest`,
    `HttpAiClientTest` (401 message/exception free of the key).
  - retry-on-timeout: `ResilientAiClientTest` (retries then succeeds /
    exhausts, delegate call counts).
  - audit-emitted: `ResilientAiClientTest` (exactly one SUCCESS/
    RETRY_EXHAUSTED/BLOCKED event per call, with requestId/provider/model).
  - plus `RetryPolicyTest`, `CircuitBreakerTest`, `HexagonalBoundaryTest`
    (ArchUnit: ports + resilience are free of Spring/SLF4J).
- `AGENTS.md` — corrected the build image to `gradle:8.14.5-jdk21-ubi`
  (the documented `gradle:8.10.2-jdk21` fails with Spring Boot 4.1.0, which
  requires Gradle 8.14+).

## Testing

- `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.14.5-jdk21-ubi gradle :libraries:common-ai:clean :libraries:common-ai:build -Pversion=0.0.1 --no-daemon` — BUILD SUCCESSFUL (24 tests, 0 failures).
- `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.14.5-jdk21-ubi gradle clean build -Pversion=0.0.1 --no-daemon` — BUILD SUCCESSFUL (whole repo, 36 tasks).
- Note: the prompt's `gradle:8.10.2-jdk21` command fails before any compile
  ("Spring Boot plugin requires Gradle 8.14 or later"); verified the Gradle
  8.14.5 image (AGENTS.md baseline) is the correct one.

## Risks

- `HttpAiClient` is OpenAI-compatible JSON (`chat/completions` shape); other
  providers need their own transport or a payload adapter — follow-up for
  AI-1..AI-7.
- `ResilientAiClient` retry backoff blocks the calling thread (lab scale);
  acceptable, noted in code. Real deployments would use a reactive/reactive
  scheduler.
- Audit events are log lines routed via logger `com.finpay.ai.audit`; wiring
  that logger to an append-only audit sink is a deployment concern.
- `gradle/wrapper/gradle-wrapper.properties` still pins 8.10.2; running
  `./gradlew` locally will fail until bumped to 8.14+ (follow-up, not touched
  here to avoid an unverifiable change).