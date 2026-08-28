// Package store is the Postgres access layer: the pgx pool, the embedded
// migrations applied on startup and, as the milestones progress, the queries
// (hand-written SQL until M4, when sqlc arrives).
package store

// SystemUserID is the single user seeded by migration 0001. It owns all data
// until M3 introduces real accounts (see ADR-0005).
const SystemUserID = "00000000-0000-0000-0000-000000000001"
