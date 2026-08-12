# Security

Security is a first-class architecture concern (§28). This document is the
contract; implementation lands per-phase.

## Identity & access

- **Dedicated OIDC Identity Provider** (Keycloak in lab, Auth0/Okta in prod
  analogy). We do **not** build an OAuth server (ADR — identity integration).
- Flow: Client → IdP → **access token (JWT)** → API Gateway → microservices.
- **Gateway enforces authentication** (validates JWT signature, expiry,
  issuer/audience) and attaches the verified principal to the request context
  (correlation + subject + roles/scopes).
- **Service-to-service:** short-lived JWT (client-credentials) or mTLS where
  justified (§28). Internal gRPC (risk eval) uses mTLS in non-lab tiers.

## Authorization (RBAC)

- Roles: `CUSTOMER`, `OPERATOR`, `AUDITOR`, `ADMIN`, plus service roles for
  machine clients.
- Scopes gate fine-grained actions (`payment:create`, `transfer:create`).
- Enforced at **gateway** (coarse) and **service** (fine, domain-aware) — defense
  in depth. Never trust a downstream call that bypassed the gateway for
  external traffic.

## Secrets & encryption

- **No secrets in Git** (Rule, §28). `.env.example` only; real values via
  Kubernetes Secrets (lab) / Vault or cloud KMS (prod, documented as the
  upgrade path).
- Data in transit: **TLS everywhere** (ingress, service mesh, Kafka, DB, Redis).
- Data at rest: encrypted volumes; PII/PII-like fields minimized; KYC docs
  encrypted at the application layer.
- **Token validation strategy:** gateway validates signature with IdP JWKS,
  caches JWKS with short TTL, checks `exp`/`iss`/`aud`/`azp`. Services re-verify
  on sensitive endpoints.

## Rate limiting & abuse

- Gateway: per-client / per-token / per-IP rate limits (token-bucket in Redis).
- Critical financial endpoints additionally rate-limited and idempotency-key
  gated.
- **WAF** (see `security/waf/`, §29) sits **before** the gateway: protects
  against SQLi, XSS, malicious payloads, request-size limits, bot/abuse. WAF is
  **defense-in-depth**, never a replacement for app-level validation.

## Input validation & headers

- All external input validated at the boundary (bean validation / schema).
- Security headers (CSP, HSTS, X-Content-Type-Options, etc.) set at gateway.
- Internal exceptions never leak to clients — consistent error model
  (`code`, `message`, `traceId`), §25.

## Audit

- Security-sensitive operations emit immutable audit events
  (WHO/WHAT/WHEN/RESOURCE/ACTION/RESULT/CORRELATION_ID) → `audit-service`
  (append-only). See `docs/architecture/...` + audit-service.

## Scanning

- CI: **SAST**, dependency scanning, container scanning, secret scanning
  (§35, §28). Failing scans block promotion.

## Network segmentation (K8s)

- `NetworkPolicy` per namespace: gateway can reach services; services reach only
  their DB/cache/Kafka; cross-service sync calls limited to needed pairs
  (e.g. transfer → risk via gRPC only).
