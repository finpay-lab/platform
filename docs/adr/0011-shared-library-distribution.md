# ADR-0011 — Shared-library distribution across repositories

**Status:** Accepted
**Date:** 2026-08-12
**Deciders:** Hermes (Architect)

## Context

`finpay-lab/platform` owns the shared libraries (`com.finpay:common-web`,
`common-security`, `common-observability`, `common-test`). The sibling service
repositories (`account-service`, etc.) must consume them. Two distribution
mechanisms were evaluated:

1. **GitHub Packages (Maven registry)** — publish `com.finpay:*` to
   `maven.pkg.github.com/finpay-lab/platform` and have services resolve them as
   remote dependencies.
2. **Gradle composite build via git submodule** — each service vendors
   `platform` as a git submodule and `includeBuild`s it, so the libraries are
   built from source and resolved in-project.

## Decision

**Adopt option 2 (composite build + git submodule).** Each service repo contains
`platform` as a git submodule at `finpay-platform/` and declares
`includeBuild 'finpay-platform'` in its `settings.gradle`. Services depend on
`com.finpay:common-web:0.0.1` etc. exactly as if they were published artifacts,
but Gradle resolves them from the included build.

## Rationale

GitHub Packages was attempted and **failed in this environment**: while POMs and
metadata uploaded successfully (HTTP 200/302), the library **JARs were never
stored** by `gradle publish` (`from components.java`) — a known Gradle +
GitHub Packages interaction where the jar artifact is dropped (the jar also
carries a `plain` classifier because `bootJar` is disabled in libraries, and the
unclassified `*.jar` path 404s). Credential plumbing for GitHub Packages from the
sandboxed Gradle/Docker build was also fragile (env-var expansion inside the
Gradle daemon did not work; only literal `-Pgpr.token` flags authenticated, and
resolution additionally required credentials in `settings.gradle`).

Given the laboratory goal is to *demonstrate the architecture*, not to operate a
package registry, the composite-build approach:
- **Works reliably** in this environment (proven: `account-service` and
  `customer-service` build clean from a fresh GitHub clone with `--recurse-submodules`).
- **Needs no registry, no credentials, no publish step** for consumers.
- Is a **legitimate real-world pattern** — many organisations vendor internal
  libraries via composite builds / source submodules to avoid registry friction
  during early development.
- Keeps the multi-repo topology intact (each service is still its own repo).

## Consequences

- A change to a shared library requires updating the submodule reference in each
  consuming service (`git submodule update --remote`). This is the accepted cost
  of source-level sharing and is documented in `LOCAL_DEVELOPMENT.md`.
- `platform` remains a separate repo and the single source of truth for the
  libraries; it is still publishable to GitHub Packages later (the
  `com.finpay.publish-conventions` plugin is retained) if/when the registry path
  is needed for external consumers.
- CI for each service must check out submodules
  (`actions/checkout@v4` with `submodules: true`).

## Alternatives considered

- GitHub Packages Maven registry — rejected (jar not published in this env).
- A single monorepo — rejected (the lab specifically demonstrates multi-repo).
- JitPack / other public registries — rejected (adds external dependency; the
  lab should be self-contained).
