# System Overview

Part of: [[Home]] · Map: [[architecture/Component Map]] · Lifecycle: [[architecture/Workflow Lifecycle]]

## What it is

Minimal Temporal-style durable execution core in `org.example.flowcore` (single Maven module, Java 27, no Spring, no DB):

- `value/` — `WorkflowId`, `WorkflowType`, `ActivityId`, `TimerId` (all `record X(String value)`), `Payload(String json)` with naive `from(Object)=toString()` (`value/Payload.java:3-7`)
- `commands/` — `Command` sealed, permits 5, 4 present: `ScheduleActivity(WorkflowId, ActivityId, String activityName, Payload, ActivityOptions)`, `StartTimer(WorkflowId, TimerId, Instant fireAt)`, `CompleteWorkflow(WorkflowId, Payload)`, `FailWorkflow(WorkflowId, failureType, message)`. `CancelWorkflow` permitted but file missing — compile breaks.
- `events/` — `HistoryEvent` sealed, permits 9, 8 present: `WorkflowStarted(..., WorkflowType, Payload input, int version)`, `ActivityScheduled(..., ActivityId, String activityName, Payload, ActivityOptions)`, `ActivityCompleted(..., ActivityId, Payload result)`, `ActivityFailed(..., failureType, message, boolean retryable)`, `TimerStarted(..., TimerId, Instant fireAt)`, `TimerFired(..., TimerId)`, `WorkflowCompleted(..., Payload)`, `WorkflowFailed(..., failureType, message)`. `WorkflowCanceled` permitted but file missing.
- `context/` — `WorkflowContext` API, `WorkflowContextImpl(WorkflowId, version, HistoryReader, CommandCollector, Instant startTime)`, `HistoryReader` (cursor + `lastConsumed/hasUnprocessed/remaining/size`), `CommandCollector`, `DeterministicClock` (cursor-aware), `DeterministicRandom` (FNV-1a64 + SplitMix64, seeded from `workflowId.toString()`), `WorkflowSuspended extends Error` stackless, `NonDeterminismException extends RuntimeException`
- `replay/` — `ReplayEngine`, `ReplayResult(Suspended/Completed/Failed/Nondeterministic)`
- `workflow/` — `Workflow{void run(ctx)}`, `WorkflowRegistry(HashMap, dup-reject)`
- `activity/` — `ActivityStubFactory`, `ActivityInvoker` (`activity/`, not `context/` as early sketches had)
- `config/` — `ActivityOptions(startToCloseTimeout, heartbeatTimeout, retryPolicy)` (no `defaults()`, no validation), `RetryPolicy(..., Set<String> nonRetryableExceptionTypes)` + `nextDelay(attempt)` (exists, uncalled)
- Root: `WorkflowInstance(id, type, status, input, result, version, createdAt, updatedAt)`, `WorkflowStatus(RUNNING/COMPLETED/FAILED/CANCELED)`, `Main` (empty `static void main()` with no args — not a runnable entrypoint)

No storage, no worker polling, no activity execution, no `SideEffectRecorded`/`RecordSideEffect`/`ActivityFailureException` (chat proposals only), no tests (`src/test` absent, no JUnit in `pom.xml`).

## How it fits together

1. Workflow implementation (`Workflow.run(ctx)`) expresses intent only via `ctx` calls.
2. `WorkflowContextImpl` translates each call into either history hit (return recorded result — only timers today) or new `Command` in `CommandCollector` + `throw WorkflowSuspended`.
3. `ReplayEngine.replay(instance, history)` creates fresh context per replay, consumes `WorkflowStarted`, runs workflow once, classifies:
   - `WorkflowSuspended` -> terminal `CompleteWorkflow/FailWorkflow`? `Completed`/`Failed` : `Suspended(commands)`
   - `NonDeterminismException` -> `Nondeterministic`
   - other `Exception` / return-without-suspend -> `Failed` (`WorkflowReturned` / `EmptySuspension` cases)
4. Future worker (see [[components/E - Storage]]) persists commands as events and schedules external work. Engine does no I/O.

## Boundaries

Does: deterministic replay skeleton, command collection, history cursor, cursor-aware time, seeded random, stub dispatch shape, timer type-order matching.
Does not: DB access, activity matching/execution, timer ID/fireAt validation, side-effect recording, retries/timeouts enforcement, versioning/migration, cancel.

Decisions: [[decisions/ADR-001 - Workflow Context Boundary]] , [[decisions/ADR-002 - Replay and Persistence Separation]], [[decisions/ADR-003 - Deterministic IDs]].

## Current truth

Does not compile — `Command.java:5-10` permits missing `CancelWorkflow`, `HistoryEvent.java:7-16` permits missing `WorkflowCanceled`. Timer `sleep()` checks types only, not IDs. `executeActivity()` (`WorkflowContextImpl.java:71-86`) unconditionally throws. See [[development/Current Status]].
