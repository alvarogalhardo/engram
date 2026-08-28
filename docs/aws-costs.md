# AWS deployment — cost notes (read before creating ANY resource)

AWS is the chosen cloud for M7 (career value). AWS is also the easiest place in
this project to accidentally spend money. Rules:

## Rule zero

**Create a Budget alarm before creating anything else.**
AWS Console → Billing → Budgets → e.g. US$ 5/month with e-mail alert at 50/80/100%.
This is the first acceptance criterion of issue M7-1, on purpose.

## Free tier reality check (changed July 2025)

AWS replaced the old "12 months free" model for **new accounts** with a
credits-based free plan (sign-up credits, limited duration, then pay-as-you-go).
What applies to *your* account depends on when it was created — verify in
Billing → Free Tier before assuming anything is free. Do not trust old
tutorials about "free t2.micro for a year" without checking.

## Stage 1 (issue M7-1): one small instance + compose

- 1× EC2 `t4g.micro`/`t4g.small` (ARM) or Lightsail smallest plan, running the
  docker-compose stack (server + Postgres on the same box) + Caddy for TLS.
- Order of magnitude: **US$ 3–10/month** without free tier/credits. Lightsail is
  the most predictable (flat price).
- Postgres data lives on the instance disk → snapshot before touching anything.

## Stage 2 (issue M7-2): the "real" architecture

- ECR + ECS Fargate (server) + RDS Postgres + GitHub Actions OIDC (no long-lived
  AWS keys in GitHub). All via Terraform in `infra/terraform/`.
- Order of magnitude without credits: **US$ 25–40/month** (RDS micro + one
  Fargate task + NAT decisions dominate; avoid NAT Gateway — public subnet +
  security groups is fine for this project and saves ~US$ 32/month).
- Decommission stage 1 when stage 2 is live. Never run both.

## Habits

- Tag everything `project=engram`; use Cost Explorer filtered by that tag.
- Tear down experiments the same day (`terraform destroy` works and is course
  material too).
- The app keeps working offline forever — a paused backend costs nothing and
  breaks nothing.
