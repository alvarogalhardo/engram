# ADR-0005: Static bearer token before real accounts

- **Status**: accepted
- **Date**: 2026-08-24

## Context

Sync needs an owner from day 1, but building registration/login/sessions before
ever shipping an endpoint front-loads complexity that has its own milestone
(M3, DDIA ch. 7: transactions, unique constraints, hashing).

## Decision

M1–M2 authenticate every `/v1` request with a single static bearer token from
config (`ENGRAM_API_TOKEN`, constant-time comparison), acting on behalf of one
seeded system user. M3 replaces this with argon2id accounts, opaque sessions,
and per-user scoping — as an explicit, reviewed refactor.

## Consequences

- Sync mechanics (M2) are learned without auth noise.
- The M3 refactor forces touching every query to add `user_id` scoping — a
  valuable lesson in multi-tenancy, done on purpose rather than by accident.
- The static token must never be treated as production security.
