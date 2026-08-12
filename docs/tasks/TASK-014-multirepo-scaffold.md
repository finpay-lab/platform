# TASK-014 — Multi-repository restructure: finpay-lab org

**CONTEXT**
The lab started as a single repo (`microservice-lab`, local at
`/home/ubuntu/microservice-lab`) holding the P0 foundation (4 shared libs) + event
/API contracts. The architect decided (ADR-0011, to be written by Hermes) that a
professional fintech platform uses **one repo per service** under a dedicated org,
with shared libraries published to a package registry. The GitHub org
`finpay-lab` now exists with 17 empty repos:
`platform`, `identity-service`, `customer-service`, `account-service`,
`wallet-service`, `ledger-service`, `payment-service`, `transfer-service`,
`risk-service`, `limit-service`, `notification-service`, `reconciliation-service`,
`audit-service`, `search-service`, `gateway`, `infrastructure`, `observability`.
This task mechanically establishes the repo topology. NO business logic.

**PROBLEM**
Single-repo violates Rule 1 (no shared database / independent deployability) and
is not how real fintech orgs ship services.

**GOAL**
1. Convert the local `microservice-lab` repo into `finpay/platform`: it owns the
   shared engineering libraries (`libraries/common-*`), ALL architecture docs
   (`docs/`), and the contracts (`contracts/`). Push it to `finpay-lab/platform`.
2. Scaffold each of the 16 sibling repos with a minimal, consistent Gradle
   foundation that **consumes** `com.finpay:common-*` from GitHub Packages, so
   later P1+ tasks implement services in isolation. Push each.
3. Prove the consumption pattern compiles on ONE representative repo
   (`account-service`) with a real `gradle build` in the Gradle Docker image.

**CONSTRAINTS**
- Work ONLY inside the local repo and the repos you clone into /tmp is FORBIDDEN
  by the sandbox — instead clone siblings into subdirs under
  `/home/ubuntu/microservice-lab/_scaffold/<name>` or operate via `gh` API +
  local working copies under the home dir. Do NOT write to `/tmp` for scratch
  Gradle projects (that triggers auto-reject + kills the run).
- Keep the repo buildable. Do NOT change the tech baseline (Java 21 / Spring
  Boot 3.3.5) without an ADR.
- GitHub Packages Maven repo must be declared in each sibling's
  `settings.gradle` / `build.gradle` pointing at
  `https://maven.pkg.github.com/finpay-lab/platform` (uses the existing GH token
  via `~/.config/gh/hosts.yml` or `GITHUB_TOKEN` env — read it from the gh CLI,
  do not hardcode secrets in committed files; use a `gradle.properties` that is
  gitignored, or reference env vars).
- Each sibling repo is independent (own git, own CI file). No cross-repo file
  references.

**ARCHITECTURE**
- `finpay/platform` = the only repo containing `libraries/common-*` + `docs/*`
  + `contracts/*` + the `build-logic` convention plugins (published as a
  convention-plugin artifact too, OR each sibling duplicates the small
  convention-plugin set — choose duplication for true independence; document the
  choice). Recommendation: each sibling carries its OWN copy of the 4 flat
  convention plugins (they are tiny) so services are independently buildable
  without depending on platform's build internals; services depend ONLY on the
  published `com.finpay:common-*` jars.
- Sibling repo layout:
  ```
  <service>/
    settings.gradle        (includes build-logic, rootProject.name)
    build.gradle
    gradle.properties      (gitignored: contains github packages creds via env)
    gradle/libs.versions.toml
    gradle/wrapper/...
    build-logic/           (own copy of 4 convention plugins)
    .github/workflows/ci.yml
    src/main/java/com/finpay/<svc>/...   (a placeholder Application class + one
        trivial @SpringBootTest-free smoke test OR a constant test, to prove build)
    README.md
    AGENTS.md              (copy of platform AGENTS.md, adjusted for the service)
  ```

**FILES TO TOUCH**
- Local `microservice-lab`: `git remote set-url origin https://github.com/finpay-lab/platform.git`,
  commit (add a short note that this is now `platform`), push to `finpay-lab/platform` main.
- For each of the 16 siblings: create files above, `git init`, commit, `gh repo`
  already exists so just `git remote add origin` + push (do NOT `gh repo create`
  again — repos exist).
- Add `.gitignore` per sibling (reuse platform's).

**ACCEPTANCE CRITERIA**
- `finpay-lab/platform` contains the 4 libs + docs + contracts and is pushed.
- All 16 sibling repos exist on GitHub with a committed Gradle foundation.
- `account-service` builds GREEN via the Gradle Docker image and resolves
  `com.finpay:common-web` (or another common-*) from GitHub Packages — proving
  cross-repo lib consumption works.
- No `/tmp` writes; no business logic; build stays green where built.

**TEST REQUIREMENTS**
- `account-service`: `docker run --rm -v "$PWD":/work -w /work -v gradle-cache:/root/.gradle gradle:8.10.2-jdk21 gradle build` → BUILD SUCCESSFUL and shows
  `common-web` resolved from `maven.pkg.github.com/finpay-lab/platform`.

**NON-GOALS**
- No service business logic (no controllers, no aggregates beyond a placeholder).
- No Kubernetes/Terraform/Ansible content in this task (those repos stay empty
  placeholders for later phases).
- Do NOT write ADR-0011 (Hermes owns that).

**VALIDATION COMMANDS**
- platform: `git push origin main` (verify via `gh api repos/finpay-lab/platform`)
- account-service: the docker gradle build above → BUILD SUCCESSFUL.

**ENGINEER REPORT FORMAT**
- repos pushed (list)
- how GitHub Packages creds were supplied without committing secrets
- account-service build result + proof common-* resolved from packages
- failures / concerns
