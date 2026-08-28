# ADR-0004: Sync v1 = LWW + tombstones + server-assigned sequence cursor

- **Status**: accepted
- **Date**: 2026-08-24

## Context

The app is offline-first; the server is the sync point between devices. Proper
multi-master conflict resolution (version vectors, CRDTs) is deep water for a
first implementation, and DDIA (ch. 5) teaches *why* last-write-wins is both
popular and dangerous. Review history, unlike decks/cards, is append-only.

## Decision

- **Decks/cards**: last-write-wins on `updated_at` (server clock wins ties),
  deletes are tombstones (`deleted` flag), never row deletion.
- **Pull cursor**: a server-assigned monotonic `server_seq` (bigserial) — a
  logical clock, not wall time. Clients pull "everything after seq N".
- **Review logs**: append-only events with client-generated UUIDs, deduplicated
  on insert. They cannot conflict by construction.
- Version vectors / CRDTs are **deliberately deferred**; the sync protocol doc
  records the known LWW anomalies (lost updates under concurrent edits) as an
  accepted trade-off for v1.

## Consequences

- Simple to implement and reason about; testable with pre-written spec tests.
- Concurrent edits to the same card on two offline devices lose one side. For a
  single-user study tool this is acceptable; fixing it honestly is future course
  material (and the reason this ADR exists).
