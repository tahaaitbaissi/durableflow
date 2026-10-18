---
type: component
status: drafted-unverified
milestone: M2
---

# Component D — Replay Engine

## Purpose

Re-execute workflow code against recorded history to reconstruct state and determine next commands. Pure function `(instance, history) -> ReplayResult`; no I/O.

## Responsibilities

- `Workflow` interface (`workflow/Workflow.java:5-7`): `void run(WorkflowContext ctx) throws Exception`. `void` return is deliberate — termination is via `ctx.complete()/fail()`, not return value.
- `WorkflowRegistry` (`workflow/WorkflowRegistry.java:10-39`): `HashMap<WorkflowType, Workflow>`, `register` null/dup checks (`IllegalArgumentException` on dup), `get` missing -> `IllegalArgumentException`. Plain `HashMap`, not `ConcurrentHashMap` as some chat sketches showed; register-before-replay only, not thread-safe.
- `ReplayEngine(registry)` (`replay/ReplayEngine.java:21-126`): `replay(instance, history)` — null checks, require `events.get(0) instanceof WorkflowStarted` else `Failed("InvalidHistory")`, `registry.get` inside try/catch `RuntimeException` -> `Failed`, `reader.next()` consumes start, build `WorkflowContextImpl(id, version, reader, collector, started.recordedAt())`, `workflow.run(ctx)` in try/catch.
- `ReplayResult` (`replay/ReplayResult.java:9-30`): sealed, `Suspended(commands)` (defensive `List.copyOf`), `Completed(Payload)`, `Failed(failureType,message)`, `Nondeterministic(message)`. No `permits` clause — all nested records in one file, so no missing-type break (unlike `Command`/`HistoryEvent`).
- Catch order: `WorkflowSuspended` (Error) -> `classify(snapshot())`; `NonDeterminismException` -> `Nondeterministic`; `Exception` -> `Failed(class-name, message-or-default)`; normal return -> `Failed("WorkflowReturned")`.
- `classify()`: empty -> `Failed("EmptySuspension")`; last=`CompleteWorkflow` -> `Completed(result)`; last=`FailWorkflow` -> `Failed(failureType(), message())`; else `Suspended(commands)`. Uses `failed.failureType()` — matches `FailWorkflow` record.

## Non-responsibilities

- No persistence, no activity execution, no task polling, no `recordedAt` assignment (see [[decisions/ADR-002 - Replay and Persistence Separation]]).

## Dependencies

- `WorkflowRegistry`, `WorkflowContextImpl`, `HistoryReader`, `CommandCollector`, `WorkflowInstance`, `ReplayResult`, `WorkflowStarted`.

## Inputs / Outputs

- Inputs: `WorkflowInstance(id, type, status, input, result, version, createdAt, updatedAt)` + ordered `List<HistoryEvent>`.
- Outputs: exactly one `ReplayResult`. `Suspended` carries commands for future worker to persist; `Completed/Failed` are intents until worker appends terminal event.

## Invariants

- Fresh context per replay; `reader` and `collector` never reused.
- Same input + compatible history -> same decisions (untested).
- Completed activities return recorded results, never re-execute (blocked — `executeActivity` throws).
- New work appears only as commands.
- `version` passed through to context but never branched on — no versioning logic yet.

## Implementation status

Drafted, unverified. Constructor/accessor compatibility flagged in chats is **now resolved**: `ReplayEngine` passes `(WorkflowId, int, HistoryReader, CommandCollector, Instant)` matching `WorkflowContextImpl` ctor; `Completed.result()` and `FailWorkflow.failureType()/message()` match records. Remaining gaps are behavioral, not signature:

- Project still does not compile (missing `CancelWorkflow` / `WorkflowCanceled` in other files) even though `replay/` itself is self-consistent.
- No tests; activity path throws; cancel path unhandled (`WorkflowStatus.CANCELED` exists, no `ReplayResult.Canceled`, no cancel command/event files).
- Leftover-history check deliberately omitted: `HistoryReader.hasUnprocessedHistory()/remaining()` exist (`HistoryReader.java:39-45`) but `ReplayEngine` never calls them. Chats explicitly decided to defer blanket leftover check until event protocol defines which trailing events are valid. Docs previously implying engine checks this were wrong.
- `registry.get` uses `HashMap`; concurrent register+replay is unsafe. Registration must finish before workers start.
- `Workflow.run` returning normally without `complete/fail/suspend` -> `Failed("WorkflowReturned")` by design (not silent RUNNING).

## Known issues

- Add `hasUnprocessedHistory` policy only at correct boundaries (per-operation matching), not blanket on every suspend — current omission is intentional.
- `get()` throwing `IllegalArgumentException` maps to `Failed` — confirm vs `Nondeterministic` for version-skew; current code maps any `RuntimeException` from registry to `Failed`.
- Handle `CancelWorkflow` / `CANCELED` or remove permits (blocks compile, not engine logic).
- No `Clock` injection in engine; `recordedAt` for future events is worker's job (M3).

## Tests

- [ ] Empty history / first-not-`WorkflowStarted` -> `Failed("InvalidHistory")`.
- [ ] Unknown `WorkflowType` -> `Failed` (registry `IllegalArgumentException` mapped).
- [ ] Timer: empty history + `sleep()` -> `Suspended([StartTimer])`; `TimerStarted` only -> `Suspended` no-dup; `Started+Fired` + `complete()` -> `Completed`.
- [ ] Activity with recorded result replays (blocked on `executeActivity`).
- [ ] Order mismatch -> `Nondeterministic` (blocked on `executeActivity`).
- [ ] `complete`/`fail` -> terminal results; return-without-suspend -> `Failed("WorkflowReturned")`; empty suspension -> `Failed("EmptySuspension")`.
- [ ] `fireAt` stability across re-replay (same history -> same `StartTimer.fireAt`).

## Related notes

- [[architecture/Deterministic Replay]], [[components/B - Workflow Context]], [[components/C - Activity Stubs]], [[development/Testing Strategy]]
