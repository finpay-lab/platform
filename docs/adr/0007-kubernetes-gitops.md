# ADR-0007: Kubernetes Deployment & GitOps (Argo CD)

## Status
Accepted

## Context
Need reliable, repeatable, observable deployment of many services (§30–§32).

## Decision
- **Kubernetes** as the runtime: Deployment/Service/ConfigMap/Secret/Ingress/
  HPA/PDB/NetworkPolicy/ServiceAccount/RBAC, with distinct startup/liveness/
  readiness probes.
- **Argo CD GitOps**: desired state lives in Git; Argo reconciles. No manual
  `kubectl apply` to prod-like envs. Application per env; ApplicationSet for
  fleet. Automated sync + prune + self-heal; rollback = Git revert.
- **CI** builds/tests/scans, pushes **immutable image tags** to a registry, then
  updates the GitOps repo (image tag) — Argo picks it up.

## Why GitOps
- Auditable, revertible, drift-detected deployments.
- Clear promotion path dev→staging→prod via `values-*.yaml`/branches.

## Terraform vs Ansible (ADR-0008/0009)
K8s apps = GitOps. Terraform = infrastructure. Ansible = host bootstrap.
