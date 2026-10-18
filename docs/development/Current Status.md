# Current Status

Last reviewed: 2026-10-18 (verified against `src/main/java/org/example/flowcore/` — 30 `.java` files read; `mvn` unavailable in this env so compile not re-run, missing `permits` targets confirmed absent by glob).

## Current milestone

M2 — Workflow Context and Replay Engine (timer leg partial, activity leg blocked).

## Present in codebase (code exists, behavior NOT yet verified by tests — no `src/test`, no JUnit in `pom.xml`)

- [x] Command/event records with exact shapes: `ScheduleActivity`, `StartTimer`, `CompleteWorkflow(result())`, `FailWorkflow(failureType(),message())`, `WorkflowStarted`, `ActivityScheduled/Completed`, `ActivityFailed(...,retryable)`, `TimerStarted(fireAt)`, `TimerFired`, `WorkflowCompleted/Failed`. `Payload.from(Object)=toString()`, no `toObject`.
- [x] Identity/values: `WorkflowInstance`, `WorkflowStatus`, `value/{WorkflowId,WorkflowType,ActivityId,TimerId,Payload}`.
- [x] `WorkflowContext` API + `WorkflowContextImpl`: `sleep` (type-order only), `now()` via `clock.now()`, `random*()` via seeded PRNG, `complete()` via `Payload.from`, `fail()`, `newActivityStub()` delegation. Ctor `(WorkflowId,version,HistoryReader,CommandCollector,Instant)` matches `ReplayEngine` call.
- [x] `HistoryReader` full API (`peek/next/lastConsumed/position/hasUnprocessed/remaining/size`), `CommandCollector(snapshot=List.copyOf)`.
- [x] `DeterministicClock` (cursor-aware, `lastConsumed.recordedAt()` else start), `DeterministicRandom` (FNV-1a64 seed from `workflowId.toString()`, SplitMix64).
- [x] `ActivityStubFactory` + `ActivityInvoker` (interface check, `Object`-method handling, default-method reject, `args==null->new Object[0]`).
- [x] `Workflow{void run(ctx)}`, `WorkflowRegistry(HashMap, dup-reject, missing->IllegalArgumentException)`, `ReplayEngine.replay/classify`, `ReplayResult(Suspended/Completed/Failed/Nondeterministic)` with correct `failureType()` accessor.

## In progress (code incomplete or broken)

- [ ] Fix compilation: add `CancelWorkflow.java` + `WorkflowCanceled.java` or remove from `permits` (`Command.java:5-10`, `HistoryEvent.java:7-16`). Blocks `mvn compile`.
- [ ] Implement `WorkflowContextImpl.executeActivity` (`WorkflowContextImpl.java:71-86` throws unconditionally). All stub paths dead-end.
- [ ] Add `sleep()` ID/fireAt validation if adopted (currently type-only); decide zero-duration policy (currently allowed).
- [ ] Decide cancel semantics (`CANCELED` status vs no `ReplayResult.Canceled`).
- [ ] Replace naive `Payload.from` (NPE on null, lossy `toString()`); add `ActivityOptions.defaults()` if wanted (chat examples use it, code lacks it).
- [ ] Fix/define `Main` (`Main.java:4` is `static void main()` — no `String[] args`, not runnable) or delete it.
- [ ] Seed clarification: `workflowId.toString()` vs `workflowId.value()` for `DeterministicRandom` (current couples to record-`toString`).

## Not yet verified (no `src/test` exists)

- [ ] Recorded activity result returns correctly (blocked).
- [ ] Missing activity causes suspension without duplicate scheduling (blocked).
- [ ] Order mismatch -> nondeterminism (timer type-mismatch works; activity swap blocked; ID-mismatch never, since IDs unchecked).
- [ ] Timer replay across tasks (logic present, untested; ID/fireAt unchecked).
- [ ] Random stability across replay (logic present, untested; insertion shifts silently).
- [ ] Terminal `complete/fail` mapping, `WorkflowReturned`/`EmptySuspension`/`InvalidHistory` guards.
- [ ] `compile` green after permits fix.

Resolved since last review:

- [x] `WorkflowContextImpl` ctor vs `ReplayEngine` construction — now match (earlier chat mismatch fixed).
- [x] `now()` delegates to `clock.now()` (earlier `history.peek().recordedAt()` NPE fixed).
- [x] `complete()` uses `Payload.from` (earlier `(Payload)` cast fixed).
- [x] `FailWorkflow` accessor is `failureType()` and engine uses it (earlier `type()` confusion fixed).

## Immediate next task

1. Fix sealed `permits` gaps so project compiles, then implement `executeActivity` mirroring `sleep()` including the ID/name comparison policy, so `WorkflowContextImpl`, `HistoryReader`, and `ReplayEngine` share one replay protocol.

## Update policy

Mark complete only when code is present **and** a test verifies it. Chat sketches (`SideEffectRecorded`, `RecordSideEffect`, `ActivityFailureException`, `CommandApplier`, workers, `ActivityOptions.defaults()`, `Payload.toObject`) are proposals until files exist.
