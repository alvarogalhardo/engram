# ADR-0003: REST/JSON first, protobuf/gRPC as a later evolution

- **Status**: accepted
- **Date**: 2026-08-24

## Context

The client-server contract needs an encoding. DDIA chapter 4 (Encoding and
Evolution) is best appreciated by *evolving* a live API, not by starting with the
"right" answer.

## Decision

v1 API is REST + JSON, documented in `server/api/openapi.yaml`. Milestone M4
introduces schema-evolution pressure on purpose (a compatible field addition, a
versioning policy ADR) and a gRPC/protobuf spike with a measured JSON-vs-proto
comparison. Adoption is decided by the numbers and recorded in an ADR.

## Consequences

- Day-1 debugging is `curl`-friendly; the Android client needs no codegen.
- We accept paying an encoding migration later — deliberately, as course material.
