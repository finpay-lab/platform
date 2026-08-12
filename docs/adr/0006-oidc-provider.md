# ADR-0006: OAuth/OIDC via Dedicated Identity Provider

## Status
Accepted

## Context
FinPay needs authentication/authorization. Building an OAuth server is out of
scope (brief: "Do not implement an OAuth server from scratch").

## Decision
Integrate a **dedicated OIDC Identity Provider** (Keycloak in lab; Auth0/Okta/
Cognito analogy in prod). `identity-service` integrates (maps external subject →
internal principal, roles/permissions, session metadata) but does **not**
implement token issuance.

## Token validation
- Client obtains JWT from IdP.
- **API Gateway validates** signature/JWKS, `exp`, `iss`, `aud`, `azp`.
- Internal services re-verify on sensitive endpoints; propagate principal via
  trusted headers / context (mTLS or gateway-signed).

## Why not build it
Security-critical, easy to get wrong, already solved by mature IdPs. Effort is
better spent on fintech distributed-systems teaching.

## Service-to-service
Short-lived JWT (client-credentials) or mTLS (§28). gRPC risk eval uses mTLS in
non-lab tiers.
