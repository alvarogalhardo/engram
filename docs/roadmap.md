# Roadmap — milestones, issues, and what they teach

Each milestone maps to chapters of *Designing Data-Intensive Applications*.
Issues labeled `mentored` are implemented by Alvaro; the pre-written skipped
tests in the tree are their definition of done.

---

## M1 — Hello, server (DDIA ch. 1–3)

*Goals: HTTP services in idiomatic Go, relational modeling, hand-written SQL,
table-driven tests.* Reference code to study first: `internal/config`,
`internal/api/healthz.go` and their tests.

1. **Model decks & cards in Postgres (migration 0002)** `server` — Write
   `migrations/0002_decks_cards.{up,down}.sql` implementing sync-protocol.md §2:
   UUID PKs, FK to the seeded user, `updated_at`/`deleted`/`server_seq` (one
   global sequence shared by synced tables). *AC: migrations apply and roll back
   cleanly on a fresh DB; schema matches the spec table.*
2. **Decks CRUD** `server` — `GET/POST /v1/decks`, `GET/PUT/DELETE /v1/decks/{id}`.
   DELETE sets the tombstone. *AC: un-skip `decks_test.go`; green with `-race`.*
3. **Cards CRUD** `server` — `GET/POST /v1/decks/{id}/cards`, `GET/PUT/DELETE
   /v1/cards/{id}`. *AC: un-skip `cards_test.go`; 404/422 semantics per tests.*
4. **Bearer auth middleware** `server` — Compare `Authorization: Bearer` against
   config token with `crypto/subtle.ConstantTimeCompare`; wrap `/v1`; `/healthz`
   stays public. *AC: un-skip `auth_test.go`.*
5. **OpenAPI for all M1 endpoints** `docs` — Extend `server/api/openapi.yaml` to
   match real behavior. *AC: spec validates; every implemented route documented.*

## M2 — Sync v1: offline-first (DDIA ch. 5, revisiting 3)

*Goals: replication intuition, LWW and its dangers, logical vs wall clocks,
tombstones, idempotency.* Read `docs/sync-protocol.md` end-to-end first.

6. **Room migration: sync metadata** `android` — Add `syncId`/`updatedAt`/`dirty`/
   `deleted` to Deck and Card (Room migration 1→2); convert delete flows to soft
   delete. *AC: migration test passes; the 39 existing tests stay green.*
7. **`POST /v1/sync/push`** `server` — Batched LWW upserts per spec §4, fresh
   `server_seq` per applied write, idempotent retries, `stale` list in response.
   *AC: un-skip push cases in `sync_test.go` (includes clock-skew and idempotency
   cases).*
8. **`GET /v1/sync/pull`** `server` — Cursor on `server_seq`, pagination,
   tombstones included (spec §5). *AC: un-skip pull cases; push→pull round-trip
   test green.*
9. **Review upload (append-only)** `server` — Spec §6, `ON CONFLICT DO NOTHING`.
   The issue asks you to answer in the PR: *why can this endpoint never
   conflict?* *AC: un-skip review cases.*
10. **Android sync engine** `android` — Ktor client + session orchestration
    (spec §7), cursor in DataStore, merge logic as pure functions. *AC: un-skip
    the provided merge-logic unit tests; manual test: edit offline on device A →
    sync → appears on device B (or second emulator).*
11. **Background sync + "Sync now"** `android` — WorkManager periodic sync with
    constraints + manual button and status/error surface in Settings. *AC: manual
    sync works from the UI; failed sync shows a human-readable error.*

## M3 — Accounts & devices (DDIA ch. 7)

12. **Registration & login** `server` — argon2id (`x/crypto`), opaque session
    tokens (256-bit random, stored hashed), registration inside a transaction
    relying on the unique index for races (not check-then-insert). *AC: un-skip
    auth account tests, including the duplicate-email race test.*
13. **Scope everything by user** `server` — Every query gains `user_id`; cursors
    become per-user; static token removed. *AC: un-skip the cross-user isolation
    test — user B must never see user A's rows.*
14. **Devices + login UI** `server` `android` — `devices` table; login screen;
    token stored in encrypted DataStore. *AC: two emulators, one account, both
    sync; session survives process death.*

## M4 — Encoding & evolution (DDIA ch. 4)

15. **API versioning policy + one compatible evolution** `docs` `server` — Write
    ADR-0006 (versioning strategy), then add `tags` to cards forward- and
    backward-compatibly; an old client must keep syncing. *AC: compatibility
    matrix tests green.*
16. **gRPC/protobuf spike on pull** `server` `spike` — Implement pull as gRPC in
    parallel, measure payload sizes and latency vs JSON, write the numbers into
    `docs/notes/grpc-spike.md`, decide adopt/shelve in an ADR.
17. **Adopt sqlc** `server` — Convert the store layer; reflect in the PR on what
    codegen buys after having written the plumbing by hand. *AC: full suite stays
    green.*

## M5 — The log (DDIA ch. 11)

18. **Events table + transactional outbox** `server` — Every mutation records a
    domain event in the same transaction; review uploads become first-class
    events. *AC: un-skip the outbox invariant test (no state change without an
    event).*
19. **Consumer → projections** `server` — A goroutine tails the events table
    (offset tracking), maintaining per-user stats projections, rebuildable from
    scratch. *AC: un-skip the replay property test (replay-from-zero ==
    incremental).*
20. **Server stats endpoint + client comparison** `server` `android` — Serve the
    projections; the Android stats screen shows server vs local numbers side by
    side. Stretch (optional): swap the table tail for NATS in compose. Kafka is
    deliberately NOT recommended here — the events table teaches offsets/replay
    for free.

## M6 — Batch (DDIA ch. 10)

21. **Nightly batch worker** `server` — `cmd/engram-batch`: full scan of review
    events computing retention curves + leech detection into a reports table;
    idempotent reruns. *AC: golden-file test over the provided fixture events.*
22. **FSRS parameter fitting** `server` `spike` — Fit FSRS weights from real
    review history; document findings.

## M7 — Ship it: AWS (can start any time after M2)

Read `docs/aws-costs.md` FIRST. Budget alarm before any resource.

23. **Stage 1: one instance + compose** `infra` — EC2 t4g or Lightsail, compose
    stack + Caddy TLS, deploy by SSH + `docker compose pull`. *AC: budget alarm
    exists (screenshot in PR); `https://.../healthz` green from mobile network;
    runbook in `infra/README.md`; real monthly cost recorded.*
24. **Stage 2: Terraform + ECS Fargate + RDS + OIDC** `infra` — ECR, Fargate
    service, RDS Postgres, GitHub Actions OIDC role (no long-lived keys). *AC:
    `terraform plan` clean in CI; push-to-main deploys; stage 1 decommissioned.*

## M8 — Android quality

25. **i18n: extract strings, EN + pt-BR** `android` — Move hardcoded composable
    strings to resources; enable the HardcodedText lint as error. *AC: app runs
    in both locales; lint clean.*
26. **Export decks to .apkg** `android` — Inverse of the importer (SQLite + zip +
    media). *AC: un-skip the provided round-trip test (export → import →
    lossless).*
27. **Scheduler interface + FSRS** `android` — Extract a `Scheduler` interface
    from `Sm2Scheduler`, implement FSRS behind it, settings toggle. *AC: SM-2
    tests untouched and green; FSRS unit tests added.*
28. **Screenshot tests** `android` `spike` — Roborazzi or Paparazzi on the main
    screens.
