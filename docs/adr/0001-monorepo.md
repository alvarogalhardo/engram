# ADR-0001: Monorepo for client, server, and infra

- **Status**: accepted
- **Date**: 2026-08-24

## Context

engram has an Android client, a Go server, and (later) infrastructure code. As a
solo learning project and portfolio piece, the repository itself is a product: one
place to show the story, keep API contracts next to their consumers, and run CI.

## Decision

A single public repository with `android/`, `server/`, `docs/`, and `infra/`
top-level directories. CI workflows are path-filtered so client and server changes
build independently.

## Consequences

- One README, one issue tracker, one commit history — easier to present and follow.
- API/spec changes and both sides of their implementation can land in one PR.
- Releases need per-component tag prefixes (`android-v*`); path filters must be
  kept accurate or CI runs more than needed.
