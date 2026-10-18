---
type: component
status: in-progress
milestone: M2
---

# Component B — Workflow Context

## Purpose

Deterministic facade workflow code uses instead of direct I/O, time, randomness, or persistence. Boundary between replayable user code and the outside world (see [[decisions/ADR-001 - Workflow Context Boundary]]).

## Responsibilities

- `WorkflowContext` interface (`context/WorkflowContext.java:10-23`): `newActivityStub()`, `sleep(Duration)`, `now()`, `randomUUID()`, `random()`, `getWorkflowId()`, `getVersion()`, `complete(Object)`, `fail(type,msg)`.
- `WorkflowContextImpl` (`context/WorkflowContextImpl.java:21-174`): owns `HistoryReader`, `CommandCollector`, `DeterministicClock`, `DeterministicRandom`, `ActivityStubFactory`, single `commandSequence`. Constructor is `(WorkflowId, int version, HistoryReader, CommandCollector, Instant workflowStartTime)` — matches `ReplayEngine` call site.
- `HistoryReader` (`context/HistoryReader.java:7-50`): `peek/next/lastConsumed/position/hasUnprocessedHistory/remaining/size` over `List.copyOf(events)`. No ID lookup, no skipping.
- `CommandCollector` (`context/CommandCollector.java:8-23`): ordered `add/snapshot/isEmpty`, `snapshot()` returns `List.copyOf`.
- Signals: `WorkflowSuspended extends Error` stackless (`context/WorkflowSuspended.java:3-7`), `NonDeterminismException extends RuntimeException` (`context/NonDeterminismException.java:3-9`).

## Non-responsibilities

- No DB, no activity execution, no thread sleep, no system-clock reads.

## Dependencies

- [[components/A - Commands and Events]], [[components/F - Time and Randomness]], [[components/C - Activity Stubs]].

## Inputs / Outputs

- Inputs: `WorkflowId`, `version`, `HistoryReader`, `CommandCollector`, workflow start `Instant` (taken from `WorkflowStarted.recordedAt()` by `ReplayEngine`).
- Outputs: recorded values on history hit, or new commands + `throw WorkflowSuspended`.

## Invariants

- One context per replay; never reused (`ReplayEngine` creates fresh `WorkflowContextImpl` each `replay()`).
- `commandSequence` is a single counter shared by timers and (future) activities. Currently only `sleep()` increments it (`WorkflowContextImpl.java:135`). `executeActivity()` throws before consuming a sequence number.
- `complete/fail` always suspend after emitting terminal command.
- `now()` delegates to `clock.now()` (`WorkflowContextImpl.java:111-113`), never `history.peek().recordedAt()` directly (that NPE'd on empty history — fixed).

## Implementation status

In progress, accurately:

- Done: `sleep()` type-order matching, `now()`, `randomUUID()/random()` (seeded PRNG, not recorded), `complete()` via `Payload.from(result)`, `fail()`, cursor/collector, `newActivityStub()` delegation to factory.
- `sleep()` is **partial**, not complete: checks `next == null` -> emit `StartTimer`; `next instanceof TimerStarted` -> consume, then check `peek() instanceof TimerFired`. It does **not** compare `timerId` or `fireAt` (`WorkflowContextImpl.java:150-161`). Chat proposal to validate IDs/fireAt is not implemented.
- `executeActivity()` (`WorkflowContextImpl.java:71-86`): builds `activityName = interface.getName() + "." + method.getName()`, then unconditionally throws `UnsupportedOperationException`. No history peek, no `ScheduleActivity` emit, no `ActivityCompleted/Failed` handling. Chat sketches with full match-or-suspend logic are proposals, not code.
- `complete(Object)` uses `Payload.from(result)` (`WorkflowContextImpl.java:100`), so passing `null` throws NPE inside `Payload.from` (`object.toString()`). No null guard.
- Chats proposed `SideEffectRecorded` / `RecordSideEffect` for random + `ActivityFailureException` for failures — **none exist in code**. Random is pure PRNG (see [[components/F - Time and Randomness]]).

## Known issues

- Implement `executeActivity` history matching to mirror `sleep()` (including `TimerId`/`ActivityId` comparison if adopted).
- Decide whether `sleep()` must validate `timerId`/`fireAt`; currently type-only.
- `commandSequence` discipline across timers + activities undefined once activities land.
- Trailing unconsumed history ignored (`hasUnprocessedHistory` exists but `ReplayEngine` never checks it — deliberate per chats, not a bug yet).
- `WorkflowContextImpl` allows zero-duration `sleep()` (only `null` and negative rejected). Confirm intended.

## Tests

- [ ] `sleep` new timer -> `StartTimer` + suspend (fireAt = `clock.now().plus(duration)`).
- [ ] `sleep` with `TimerStarted+TimerFired` -> return, no new command (current: type-only, no ID check).
- [ ] `sleep` with `TimerStarted` only -> suspend, no duplicate.
- [ ] `sleep` mismatch (e.g. `ActivityScheduled` where timer expected) -> `NonDeterminismException`.
- [ ] `complete/fail` -> terminal command + `WorkflowSuspended`.
- [ ] Activity replay (blocked on `executeActivity`).

## Related notes

- [[architecture/Deterministic Replay]], [[components/D - Replay Engine]], [[components/F - Time and Randomness]]
