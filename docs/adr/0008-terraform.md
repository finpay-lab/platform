# ADR-0008: Terraform for Infrastructure

## Status
Accepted

## Context
Infrastructure must be reproducible and separated from application deployment.

## Decision
**Terraform** owns infrastructure: VPC, subnets, security groups, LB, K8s
cluster, managed Postgres/Redis/Kafka, object storage, monitoring, IAM.
Structured as `modules/` + `environments/{dev,staging}`. No hardcoded secrets
(use variables + secret manager references).

## Why Terraform (not Ansible)
Declarative, stateful, provider-rich for cloud resources — ideal for
infrastructure, not host configuration.

## Relationship
Terraform builds the substrate; Kubernetes/GitOps (ADR-0007) deploys apps on
top; Ansible (ADR-0009) bootstraps any VM/agent layer Terraform doesn't own.
