# How this project is worked on

engram is a learning project with an explicit **human-implements, AI-mentors**
contract (the enforcement rules live in [CLAUDE.md](../CLAUDE.md)).

## The mechanism: skipped spec tests

Most issues are already **specified as failing tests** committed to the tree, each
marked with a skip that names its issue:

```go
func TestDeckCreateAndGet(t *testing.T) {
    t.Skip("un-skip in M1 issue: decks CRUD (docs/roadmap.md)")
    ...
}
```

That test already fixes the JSON shape, the status codes, and the edge cases
(blank name → 422, tombstoned deck → 404, and so on). So the issue never needs to
be re-explained, and "am I done?" is answered by the compiler and the test runner
instead of by asking someone.

**Removing the `t.Skip` line and making the test pass is the definition of done.**

## The loop, concretely

Taking issue #2 (*Decks CRUD*) as the worked example:

1. **Pick** the lowest-numbered open issue in the current milestone. Issues carry
   a `Blocked by #N` line when they depend on earlier work.
2. **Read** three things: the issue body, its section in
   [roadmap.md](roadmap.md), and the spec it implements
   ([sync-protocol.md](sync-protocol.md) for sync work, the test file itself for
   API shapes).
3. **Branch**: `git switch -c m1/decks-crud`.
4. **Run the tests first, still skipped**, so you see the baseline:
   `cd server && go test ./...`
5. **Un-skip** the tests for this issue and watch them fail. Now you have a
   red-green target.
6. **Implement** until green. For server work you'll want Postgres up
   (`docker compose up postgres`) and `DATABASE_URL` exported.
7. **Open a PR.** The template asks which DDIA concept the change applies —
   that answer is the study journal, and it's the part worth taking seriously.
8. **Review**: the AI comments on the diff — hints and questions first, direct
   answers only if you ask. It will not hand you the implementation.
9. **Merge** when CI is green. `main` is protected and requires it.

## Labels

| label | meaning |
|---|---|
| `mentored` | Alvaro implements; the AI may only mentor and review |
| `reference` | the AI may write it; the code exists to be read and studied |
| `spike` | timeboxed exploration, conclusions land in `docs/` |
| `server` / `android` / `docs` / `infra` | area of the codebase |

## Milestones

M1–M8 map to DDIA chapters (see [roadmap.md](roadmap.md)). Finish a milestone
before starting the next — the ordering is a learning sequence, not a backlog.
The exception is M7 (deploy), which can start any time after M2.
