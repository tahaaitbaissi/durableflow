# Workflow Engine — Engineering Notebook

## Project overview

Java + Maven durable workflow engine (`org.example.flowcore`, single module, Java 27, no Spring/DB yet).

Goal: execute long-running workflows that suspend while waiting for external work, persist progress as ordered history, and resume by replaying history. Temporal-lite direction from roadmap chats (event-sourced history, deterministic replay, durable timers, retries/backoff, saga, Postgres leases via `SELECT ... FOR UPDATE SKIP LOCKED`, crash-injection proof) — only the core replay skeleton exists so far.

Core loop (timer leg works type-only, activity leg blocked, no storage):
`HistoryEvent list` -> `ReplayEngine.replay()` -> `ReplayResult` (`Suspended` | `Completed` | `Failed` | `Nondeterministic`) -> future worker persists commands / schedules work.

Source authority: `src/main/java/org/example/flowcore/` (30 files). Chats (`Component B/C/D` sketches, M0-M9 roadmap) are proposals — docs explicitly mark what is **not** in code (`SideEffectRecorded`, `RecordSideEffect`, `ActivityFailureException`, `CommandApplier`, workers, `ActivityOptions.defaults()`, `Payload.toObject()`).

## Current focus

**Milestone:** M2 — Workflow Context and Replay Engine

Timer match-or-suspend (type-only) + `ReplayEngine` classify + seeded time/random exist. Activity `executeActivity` missing, project does not compile (`CancelWorkflow`/`WorkflowCanceled` permits gaps).

See: [[development/Current Status]] , [[development/Roadmap]] , [[development/Known Issues]]

## Navigation

- [[architecture/System Overview]] — what the system is (code-verified shapes)
- [[architecture/Component Map]] — file:line to concept map
- [[architecture/Workflow Lifecycle]] — start → suspend → resume → terminate (timer-only truth)
- [[architecture/Deterministic Replay]] — replay protocol (type-only timer check, no ID check)
- [[components/A - Commands and Events]] — `commands/`, `events/` (4+8 present, 2 permits missing)
- [[components/B - Workflow Context]] — `context/WorkflowContext*` (sleep partial, activity blocked)
- [[components/C - Activity Stubs]] — `activity/*` (factory done, execution blocked)
- [[components/D - Replay Engine]] — `replay/*`, `workflow/*` (signatures match, untested)
- [[components/E - Storage]] — missing layer, contract + chat plan only
- [[components/F - Time and Randomness]] — `DeterministicClock` (cursor-aware), `DeterministicRandom` (seeded, unrecorded)
- [[decisions/ADR-001 - Workflow Context Boundary]] , [[decisions/ADR-002 - Replay and Persistence Separation]] , [[decisions/ADR-003 - Deterministic IDs]]
- [[development/Testing Strategy]] — what is testable now vs blocked

## Engineering principles

1. Workflow code must be deterministic during replay — no `System.currentTimeMillis()`, `UUID.randomUUID()`, direct I/O (unenforced, no ArchUnit yet).
2. Side effects go through commands (`ScheduleActivity`, `StartTimer`, `CompleteWorkflow`, `FailWorkflow`). `CancelWorkflow` undecided.
3. Recorded history is source of truth for prior decisions.
4. `ReplayEngine` does no I/O, no persistence, no activity execution.
5. Significant choices get an ADR; chat proposals stay marked proposed until code lands.
6. Status is `Implemented` only when code exists **and** a test verifies it (no tests yet).

## Working rules

- Update [[development/Current Status]] when code changes.
- Add reproducible breaks to [[development/Known Issues]] with file:line.
- Link research → decision → component → test; never cite chat sketches as code.
- Open repo root as Obsidian vault; `docs/` is versioned.
