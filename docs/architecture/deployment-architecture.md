# Deployment Architecture

How the platform is deployed, from code to cluster (§30–§34, §46 Phase 11+).

## Topology (internet-facing)

```
Internet
   │
 WAF  (security/waf)  ── protects, rate-abuse, request limits
   │
 Load Balancer / Ingress Controller
   │
 API Gateway (K8s Deployment + HPA)
   │  mTLS / JWT
   ├── identity-service
   ├── customer-service
   ├── account-service
   ├── wallet-service
   ├── ledger-service
   ├── payment-service
   ├── transfer-service
   ├── risk-service
   ├── limit-service
   ├── notification-service
   ├── reconciliation-service
   ├── audit-service
   └── search-service
        │
        ├── PostgreSQL (per service, separate instances in prod)
        ├── Redis
        ├── Kafka (KRaft)
        └── OpenSearch
```

## Kubernetes primitives (per service)

- `Deployment` with resource requests/limits.
- `Service` (ClusterIP) + `Ingress` only at gateway boundary.
- `ConfigMap` (non-secret config), `Secret` (lab secret intermediary; prod →
  Vault/KMS, documented).
- **Probes:** `startupProbe` (initial warmup), `livenessProbe` (restart on
  deadlock/hang), `readinessProbe` (remove from rotation when deps down — but
  do **not** include Kafka if reads still serve).
- `HPA` (CPU + custom metrics like Kafka lag / latency).
- `PodDisruptionBudget` (min available during node drain).
- `NetworkPolicy` (default-deny; explicit allow gateway→svc, svc→own DB/cache/
  Kafka, transfer→risk gRPC).
- `ServiceAccount` + RBAC (least privilege; no broad cluster access).

## GitOps (Argo CD, §32)

```
Developer ─► Git (app repo) ─► CI (test/build/scan/push image)
                                      │
                                      ▼
                                 Registry (immutable tags)
                                      │
                                      ▼
                            GitOps repo (manifests/Helm values)
                                      │
                                      ▼
                                  Argo CD ──reconcile──► Kubernetes
```

- Argo CD `Application` per environment; `ApplicationSet` for fleet.
- Sync policy: automated, prune, self-heal. **No manual `kubectl apply` to
  prod-like envs.**
- Drift detection + rollback (Git revert → Argo syncs back).
- Promotion: dev → staging → prod via separate `values-*.yaml` + branch/tag.

## Terraform vs Ansible (§33–§34)

- **Terraform** owns *infrastructure*: VPC, subnets, security groups, LB, K8s
  cluster, managed Postgres/Redis/Kafka, object storage, monitoring, IAM.
  Modules + environments (dev/staging).
- **Ansible** owns *host/VM bootstrap & config* where K8s isn't the
  abstraction: dev/lab VM setup, OS hardening, container runtime, monitoring
  agents. Not used to deploy K8s apps (GitOps does that).

## Disaster recovery (see DISASTER_RECOVERY.md)

- DB: PITR backups; ledger immutable → reconstructable from postings.
- Kafka: retained; consumers replayable.
- Multi-AZ; PDB ensures quorum during maintenance.
