---
type: component
status: partial-broken
milestone: M2
---

# Component A — Commands and Events

## Purpose

Versioned data contract between deterministic workflow code and durable history. Commands express intent; events record what happened.

## Responsibilities

- `Command` sealed interface (`commands/Command.java:5-10`): permits `ScheduleActivity`, `StartTimer`, `CompleteWorkflow`, `FailWorkflow`, `CancelWorkflow` — 5 permitted, **4 present, `CancelWorkflow.java` missing**.
- `HistoryEvent` sealed interface (`events/HistoryEvent.java:7-16`): permits `WorkflowStarted`, `ActivityScheduled/Completed/Failed`, `TimerStarted/Fired`, `WorkflowCompleted/Failed`, `WorkflowCanceled` — 9 permitted, **8 present, `WorkflowCanceled.java` missing**.
- Carry `workflowId`, plus `sequence` + `recordedAt` on events, plus per-type payload (`ActivityId`, `TimerId`, `Payload`, `ActivityOptions`).

## Non-responsibilities

- No execution, no persistence, no matching logic (that lives in [[components/B - Workflow Context]] and [[components/D - Replay Engine]]).

## Dependencies

- `value/*` (`WorkflowId`, `WorkflowType`, `ActivityId`, `TimerId`, `Payload`), `config/ActivityOptions`.
- JDK `Instant`, `Duration`.

## Inputs / Outputs

- Inputs: workflow decisions (`ctx.sleep`, `ctx.complete/fail`, future `executeActivity`).
- Outputs: `List<Command>` from `CommandCollector.snapshot()`; persisted `List<HistoryEvent>` (future [[components/E - Storage]]).

## Invariants

- `sequence` strictly increasing per workflow; `recordedAt` non-decreasing (enforced by future store, not by records themselves).
- Intended `Command -> Event` correspondence: `ScheduleActivity->ActivityScheduled`, `StartTimer->TimerStarted`, `CompleteWorkflow->WorkflowCompleted`, `FailWorkflow->WorkflowFailed`. No `CommandApplier` implements this yet.
- Sealed `permits` must resolve — currently broken, see below.

## Implementation status

Partial, compile-broken. Present and matching proposed shapes:

- `ScheduleActivity(WorkflowId, ActivityId, String activityName, Payload input, ActivityOptions)` — matches sketch.
- `StartTimer(WorkflowId, TimerId, Instant fireAt)` — matches.
- `CompleteWorkflow(WorkflowId, Payload result)` — accessor `result()` correct.
- `FailWorkflow(WorkflowId, String failureType, String message)` — accessor is `failureType()`, not `type()`. `ReplayEngine` already uses `failed.failureType()` correctly.
- Events carry `(WorkflowId, long sequence, Instant recordedAt, ...)` as proposed.

Gaps (code truth vs chat proposals):

- `CancelWorkflow` / `WorkflowCanceled` permitted but files absent — `mvn compile` fails. Chats never defined their shape; do not assume.
- `Payload.from(Object)` exists as `new Payload(object.toString())` (`value/Payload.java:4-6`). There is **no** `toObject(Class)` / JSON round-trip. Chat sketches calling `completed.result().toObject(...)` or `Payload.from(args)` with real serialization do not apply.
- `ActivityOptions` has **no** `defaults()` factory. Chat examples using `ActivityOptions.defaults()` do not compile against current code. Constructor is `(Duration startToCloseTimeout, Duration heartbeatTimeout, RetryPolicy retryPolicy)` with no validation.
- `RetryPolicy.nextDelay(attempt)` exists but is never called; no validation on `maxAttempts`.
- Proposed in chats but absent: `SideEffectRecorded`, `RecordSideEffect` command, `ActivityFailureException`, `CancelWorkflow` handling.

## Known issues

- Add or drop `CancelWorkflow` / `WorkflowCanceled` to make `permits` resolvable.
- Decide cancel semantics (`WorkflowStatus.CANCELED` exists, `ReplayResult` has no `Canceled` variant).
- Add real `Payload` serialization; `toString()` corrupts non-string payloads and throws NPE on null.

## Tests

- [ ] Sealed-interface completeness (every permitted type loads).
- [ ] Command->event mapping table.
- [ ] `Payload` round-trip (blocked on real serialization).

## Related notes

- [[architecture/Workflow Lifecycle]], [[architecture/Deterministic Replay]], [[decisions/ADR-002 - Replay and Persistence Separation]]
