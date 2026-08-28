# ADR-0002: stdlib net/http mux and minimal Go dependencies

- **Status**: accepted
- **Date**: 2026-08-24

## Context

This project exists to *learn Go*. Frameworks hide exactly the parts worth
learning first: routing, middleware, request lifecycles, error handling, SQL.
Go 1.22+ `http.ServeMux` supports method + path-pattern routing, which removes
the traditional reason to reach for a router library.

## Decision

- HTTP: standard library only (`net/http`, `http.ServeMux` patterns). Middleware
  is hand-written (`func(http.Handler) http.Handler`).
- Database: `jackc/pgx/v5` (driver + pool) with hand-written SQL. No ORM, ever.
- Migrations: `golang-migrate` used as a library over an `embed.FS`, applied on
  server startup (no CLI install needed).
- Everything else waits until the pain justifies it. Planned, deliberate
  additions: `sqlc` (M4, after feeling the cost of hand-written query plumbing),
  `golang.org/x/crypto/argon2` (M3), protobuf/gRPC (M4 spike).

## Consequences

- More code written by hand early on — that is the point.
- If routing/middleware boilerplate becomes genuinely painful, adopting `chi` is
  a one-evening refactor; record it in a new ADR if it happens.
