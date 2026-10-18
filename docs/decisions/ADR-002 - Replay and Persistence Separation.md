# ADR-002 — Separate Replay from Persistence

- Status: Accepted
- Date: 2026-10-18

## Context

Replay must be repeatable and unit-testable without a database or external services.

## Decision

`ReplayEngine.replay(instance, history)` (`flowcore/replay/ReplayEngine.java`) receives instance + history, creates fresh `WorkflowContextImpl`, runs workflow, returns `ReplayResult`. No DB writes, no activity execution, no polling.

A separate worker / command applier (see [[components/E - Storage]]) persists commands, appends events, schedules work.

## Alternatives considered

### ReplayEngine performs database writes

Rejected: couples replay to storage, untestable in isolation.

### WorkflowContext writes directly to storage

Rejected: workflow execution would perform persistence instead of expressing decisions as commands.

## Consequences

Positive:

- Replay testable with in-memory `List<HistoryEvent>`.
- Storage can evolve independently.
- Clear `Suspended/Completed/Failed/Nondeterministic` boundary.

Negative:

- Extra infrastructure needed to apply commands idempotently.
- Worker must handle persistence failures/retries.

## Related notes

- [[components/D - Replay Engine]], [[components/B - Workflow Context]], [[architecture/System Overview]]
