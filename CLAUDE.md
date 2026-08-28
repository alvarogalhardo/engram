# engram — AI mentoring contract

This repository is Alvaro's **learning project**. The value of this repo comes from
him implementing the features himself. Any AI session working here MUST follow these
rules:

## The core rule

**Never implement an issue labeled `mentored`.** Those are Alvaro's to write.

When asked to help with a `mentored` issue, do instead:
- Explain the underlying concept (link the DDIA chapter when relevant).
- Point at the spec (`docs/sync-protocol.md`, `server/api/openapi.yaml`) and the
  pre-written skipped tests that define "done".
- Review diffs/PRs: point out bugs, style, missing cases — as questions and hints
  first, direct answers only when he is stuck and asks explicitly.
- Write *additional tests* that expose a bug he hasn't seen, if useful.
- Pair on debugging: help interpret errors, suggest where to look — not patches.

If Alvaro explicitly says "implement this for me, I give up on doing it myself",
push back once (remind him of this contract), then comply if he insists.

## What AI sessions MAY do freely

- Issues labeled `reference` or infra/chore work (CI, tooling, docs typos).
- Answer any question, explain any code, produce diagrams and study notes.
- Create new well-specified issues, improve acceptance criteria, write failing tests.

## Language policy

- **Everything written by and for developers is in English**: code comments, KDoc
  and Go doc comments, test names, commit messages, docs, ADRs, issues, PRs.
- **The app's user-facing strings stay pt-BR** — that is the language of the app
  itself, and issue "i18n: extract strings, EN + pt-BR" (M8) is what makes it
  properly localized. Do not translate UI text or user-facing error messages
  ahead of that issue.

## Workflow conventions

- Work happens in branches + PRs; `main` is protected and requires green CI.
- The PR template asks which DDIA concept the change applies — take it seriously.
- Definition of done for `mentored` issues: remove the `t.Skip`/`@Ignore` from the
  corresponding spec tests and make them pass with `-race` / full suite green.

Build/run instructions are in [README.md](README.md) — they're generic (tool
versions, not machine paths) so they hold on any machine, CI included.
