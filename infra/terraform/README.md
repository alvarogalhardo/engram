# terraform (M7 stage 2)

Planned modules (issue M7-2): VPC (public subnets, no NAT Gateway — see cost
notes), ECR repository, ECS Fargate service for `engram-server`, RDS Postgres,
IAM role for GitHub Actions OIDC deploys. State backend: S3 + lockfile
(`use_lockfile`), created manually once.
