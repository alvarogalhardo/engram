# infra

Nothing here runs before milestone **M7** — and `docs/aws-costs.md` is required
reading before creating any AWS resource (budget alarm first, always).

- `../docker-compose.yml` (repo root) — local/dev stack; also the unit of
  deployment for M7 stage 1 (single EC2/Lightsail instance + Caddy).
- `terraform/` — skeleton for M7 stage 2 (ECR + ECS Fargate + RDS + GitHub OIDC).
  Only provider pins exist today; resources arrive with issue M7-2.
