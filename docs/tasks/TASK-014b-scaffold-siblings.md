# TASK-014b — Scaffold 16 sibling service repos (multi-repo, consume published libs)

**CONTEXT**
`finpay-lab/platform` is pushed and its 4 shared libraries
(`com.finpay:common-web/-security/-observability/-test`, version 0.0.1) are
ALREADY published to GitHub Packages under `finpay-lab/platform`. The 16 sibling
GitHub repos already exist (empty) under `finpay-lab/`. This task scaffolds each
sibling with a minimal Gradle foundation that **consumes** the published libs, and
proves cross-repo consumption compiles on `account-service`. NO business logic.

**THE PUBLISH PIPELINE IS ALREADY DONE — do NOT re-publish unless you change a lib.**

**CRITICAL: how credentials work (learned the hard way)**
GitHub Packages auth from the Gradle Docker build works ONLY when the token is
passed as a plain Gradle property:
  docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle \
    gradle:8.10.2-jdk21 gradle <task> -Pversion=0.0.1 \
    -Pgpr.user=$(gh api user --jq .login) -Pgpr.token=$(gh auth token) --no-daemon
DO NOT rely on `${env.GITHUB_TOKEN}` in gradle.properties (it does not expand
reliably inside the Docker Gradle daemon). The `com.finpay.publish-conventions`
plugin reads `-Pgpr.user`/`-Pgpr.token` then falls back to GITHUB_ACTOR/
GITHUB_TOKEN env.

**SCOPE**
For each of the 16 repos (identity-service, customer-service, account-service,
wallet-service, ledger-service, payment-service, transfer-service, risk-service,
limit-service, notification-service, reconciliation-service, audit-service,
search-service, gateway, infrastructure, observability):
1. Create a working copy under `/home/ubuntu/microservice-lab/_scaffold/<name>`
   (NOT /tmp — the sandbox auto-rejects /tmp writes and kills the run).
2. Give it:
   - `settings.gradle` (rootProject.name = '<name>', includeBuild NOT needed;
     just `pluginManagement` + `dependencyResolutionManagement` repositories:
     mavenCentral AND `maven { url = uri("https://maven.pkg.github.com/finpay-lab/platform") }`).
   - `gradle.properties` (copy platform's, WITHOUT any secret; the GitHub
     Packages maven repo is declared in settings.gradle).
   - `gradle/libs.versions.toml` (minimal: java 21, springBoot 3.3.5, junit).
   - `build-logic/` = OWN copy of the 4 flat convention plugins
     (java/spring/testing/quality) + `publish-conventions` is NOT needed for
     services (only platform publishes). Services apply `spring-conventions` +
     `testing-conventions`.
   - `build.gradle` applying the conventions + depending on
     `com.finpay:common-web:0.0.1` (and common-security if it helps the demo).
   - `src/main/java/com/finpay/<svc>/<Svc>Application.java` (a `@SpringBootApplication`
     placeholder) + `src/main/resources/application.yml` (empty/server.port).
   - `src/test/java/.../SmokeTest.java` (one trivial `@SpringBootTest` context
     load test OR a plain unit test) to prove the build + lib consumption.
   - `.github/workflows/ci.yml` (build + test via the Gradle Docker image).
   - `.gitignore` (reuse platform's).
   - `README.md` (one line: what the service owns).
   - `AGENTS.md` (copy platform's AGENTS.md; adjust repo name).
3. `git init`, commit, `git remote add origin https://github.com/finpay-lab/<name>.git`,
   `git push -u origin main`. DO NOT run `gh repo create` (repos exist).
4. After all 16 are pushed, prove consumption on `account-service`:
   `cd _scaffold/account-service && docker run ... gradle build -Pgpr.user=$(gh api user --jq .login) -Pgpr.token=$(gh auth token) --no-daemon`
   It must BUILD SUCCESSFUL and resolve `com.finpay:common-web:0.0.1` from
   maven.pkg.github.com/finpay-lab/platform.

**CONSTRAINTS**
- Work ONLY inside `/home/ubuntu/microservice-lab` (use `_scaffold/<name>`).
  NEVER write to /tmp.
- No business logic (no controllers/aggregates). Placeholder app + one test only.
- Keep `bootJar { enabled = true }` for services (they are bootable).
- Do NOT change the tech baseline (Java 21 / Spring Boot 3.3.5).
- Do NOT modify `finpay-lab/platform` (already published).
- Secrets: never commit the token. Pass via `-Pgpr.token=$(gh auth token)` only
  at build time.

**ACCEPTANCE CRITERIA**
- All 16 repos on GitHub contain a committed Gradle foundation.
- `account-service` builds GREEN via Gradle Docker image and resolves
  `com.finpay:common-web:0.0.1` from the platform GitHub Packages repo.
- No /tmp writes; no business logic; no committed secrets.

**TEST REQUIREMENTS**
- account-service `gradle build` → BUILD SUCCESSFUL with common-web resolved.

**ENGINEER REPORT FORMAT**
- 16 repos pushed (list)
- account-service build result + proof common-web resolved from packages
- any repo where the build differs / failed (note, do not block others)
- failures / concerns
