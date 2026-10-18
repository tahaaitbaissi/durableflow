---
type: component
status: not-started
milestone: M3
---

# Component E — Storage

## Purpose

Durably persist history and apply `ReplayResult` commands. Explicitly **out of** `ReplayEngine` scope (see [[decisions/ADR-002 - Replay and Persistence Separation]]).

## Responsibilities (future — none implemented)

- `HistoryStore` port (to define): `append(workflowId, event)` with gap-free per-workflow `sequence` (unique constraint as single-writer guard), `loadOrdered(workflowId) -> List<HistoryEvent>`.
- `TaskQueue` port (to define): `create/claimAtomically/extendLease/completeIfOwner/releaseOrRetry/findExpired/markPoisoned`. Claim must lock + update in one transaction (`SELECT ... FOR UPDATE SKIP LOCKED` pattern from roadmap chats).
- `CommandApplier` (to define): `Suspended(commands)` -> `ActivityScheduled`/`TimerStarted` events + tasks; terminal commands -> `WorkflowCompleted/Failed` events + status update. Idempotent re-apply.
- Workers (future `H` in chats): workflow-task poller + activity-task poller, virtual threads, backoff, graceful shutdown. Activities never run inside DB transaction.
- Leases/heartbeats/reaper: `leaseOwner`, `leaseExpiresAt`, fencing `version` bumped per claim; reaper re-queues expired; activity tasks need idempotency keys (`workflowId:seq`) because at-least-once execution is unavoidable.

## Non-responsibilities

- No workflow logic, no replay decisions, no retry math (that is `RetryPolicy`).

## Dependencies

- [[components/A - Commands and Events]], [[components/D - Replay Engine]].

## Inputs / Outputs

- Inputs: `ReplayResult` + external completions (activity results, timer fires).
- Outputs: durable `List<HistoryEvent>` + claimable tasks.

## Invariants (design intent from roadmap chats, not enforced)

- Append-only; never mutate past events.
- `sequence` gap-free per workflow; duplicate `sequence` rejected.
- Same replay -> same commands -> no duplicate events on re-apply.
- One outstanding workflow task per workflow (enforced in DB, not just code).
- Terminal state (`COMPLETED/FAILED/CANCELED`) creates no further workflow tasks.

## Implementation status

Not started. Verified absent: no `store/`, no `worker/`, no `replay/CommandApplier`, no Flyway/JdbcClient, no `src/test`, no Postgres/Testcontainers. `ReplayEngine` is unit-testable with in-memory `List<HistoryEvent>` today precisely because storage is absent.

Roadmap chats (M0-M9) propose: multi-module build (`flow-core`, `flow-store-postgres`, starter, examples, chaos-tests), Flyway migrations, `JdbcClient`, in-memory port for contract tests, then Postgres impl, then workers/leases, then retries/timers, saga, starter, chaos suite. None of that exists yet — treat as plan, not code.

## Known issues

- Choose backend order: in-memory first for contract tests, then Postgres (per chats).
- Define exactly-once-effects on at-least-once delivery via idempotency keys; document ambiguity of timeouts.
- Decide where `sequence`/`recordedAt` assigned (store vs applier) and what `Clock` to inject (never `Instant.now()` in workflow path).
- Poison tasks: max attempts -> park, not infinite retry (noted in chats, no code).
- Unbounded history growth: noted as known limit in chats.

## Tests (future)

- [ ] Contract suite run against both in-memory and Postgres impls (same tests, two backends).
- [ ] Re-applying same `Suspended` twice adds no duplicates.
- [ ] Crash between replay and apply recovers; kill-9 chaos keeps invariants (every started workflow reaches terminal, histories gap-free, no double-complete).

## Related notes

- [[decisions/ADR-002 - Replay and Persistence Separation]], [[architecture/Workflow Lifecycle]], [[development/Roadmap]]
