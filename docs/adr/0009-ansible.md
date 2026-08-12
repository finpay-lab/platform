# ADR-0009: Ansible for Host/Config Bootstrap

## Status
Accepted

## Context
Some setup (lab VMs, OS hardening, container runtime, monitoring agents) is
better as config management than Kubernetes abstraction.

## Decision
**Ansible** for host/VM bootstrap and configuration: OS hardening, Docker/
container runtime install, monitoring agent setup, developer/lab env prep.

## Why not Ansible for app deploy
Kubernetes + GitOps (ADR-0007) is the better abstraction for deploying
services. Ansible stays at the host layer to avoid overlap and drift.

## Boundary
If it's a pod/deployment → GitOps. If it's an OS/VM → Ansible. Terraform (ADR-0008)
creates the VM; Ansible configures it.
