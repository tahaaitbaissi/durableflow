# Workflow Lifecycle

Parent: [[architecture/System Overview]]

## States

`WorkflowStatus`: `RUNNING`, `COMPLETED`, `FAILED`, `CANCELED`.
`WorkflowInstance`: record `(WorkflowId id, WorkflowType type, WorkflowStatus status, Payload input, Payload result, int version, Instant createdAt, Instant updatedAt)` (`WorkflowInstance.java:9-18`).

`CANCELED` has no `CancelWorkflow` file, no `WorkflowCanceled` file, and no `ReplayResult.Canceled` variant — triple gap, see [[development/Known Issues]]. Cancel is unimplementable until permits fixed and terminal mapping decided.

## Sequence (code truth)

1. **Start.** History must be non-empty with `history.get(0) instanceof WorkflowStarted` else `Failed("InvalidHistory")` (`ReplayEngine.java:36-42`). Engine consumes start via `reader.next()`, reads `started.recordedAt()` as `workflowStartTime`, builds `WorkflowContextImpl(workflowId, version, reader, collector, startTime)` (`ReplayEngine.java:57-71`). `version` passed through, never branched on.
2. **Suspend on new work (timers only today).** `ctx.sleep(duration)` with `history.peek()==null` -> `CommandCollector.add(StartTimer(workflowId, timerId(seq), clock.now().plus(duration)))` + `throw WorkflowSuspended` (`WorkflowContextImpl.java:138-148`). `timerId(seq)=workflowId.toString()+":timer:"+seq`. Engine `classify()` returns `Suspended(commands)`. Future worker must persist `TimerStarted` and, on fire, `TimerFired` — no worker exists.
3. **Resume by replay.** Next task supplies longer history. Fresh context, `DeterministicClock.now()` = `lastConsumed.recordedAt()` or start, `DeterministicRandom` reseeded from `workflowId.toString()` (record-toString, not `.value()`). Timer match is **type-order only**: `TimerStarted` -> consume; `peek() instanceof TimerFired` -> consume + return; `TimerStarted` alone -> suspend; anything else where timer expected -> `NonDeterminismException` (`WorkflowContextImpl.java:150-166`). IDs/fireAt ignored.
4. **Activities (not implemented).** Any stub call -> `executeActivity()` builds `interface.getName()+"."+method.getName()` then throws `UnsupportedOperationException`. No `ActivityScheduled` peek, no result return.
5. **Terminate.** `ctx.complete(obj)` -> `CompleteWorkflow(workflowId, Payload.from(obj))` + suspend (`WorkflowContextImpl.java:99-102`); `Payload.from(null)` NPEs. `ctx.fail(type,msg)` -> `FailWorkflow` + suspend. Engine maps last command to `Completed(Payload)` / `Failed(failureType,message)`. Return without suspend -> `Failed("WorkflowReturned")`. Suspend with no commands -> `Failed("EmptySuspension")`. `Completed/Failed` are intents until worker persists terminal event — no worker yet.

## Diagram (intended, activity leg blocked)

`WorkflowStarted -> (ActivityScheduled -> ActivityCompleted|ActivityFailed | TimerStarted -> TimerFired)* -> WorkflowCompleted|WorkflowFailed`

Cancel events/commands absent. HistoryReader exposes `hasUnprocessedHistory/remaining` but engine never enforces leftover check (deliberate deferral per chats).

## Invariants

- History append-only, ordered by `sequence()` (convention; no store enforces it yet).
- One replay = one fresh context + fresh reader/collector; no state carried across `replay()` calls.
- Terminal `ReplayResult` is not durable completion — persistence is future work.
