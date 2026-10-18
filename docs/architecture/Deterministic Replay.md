# Deterministic Replay

Parent: [[architecture/System Overview]] · Research: [[research/Deterministic Replay]] · Impl: [[components/D - Replay Engine]]

## Protocol (code truth, `ReplayEngine.java:29-99`)

`ReplayEngine.replay(instance, history)`:

1. `requireNonNull(instance/history)`; empty or `!(get(0) instanceof WorkflowStarted)` -> `Failed("InvalidHistory", ...)`.
2. `registry.get(instance.type())` inside `try/catch RuntimeException` -> `Failed(class-name, message)` on missing/dup-registry error. Registry is `HashMap`, `get` throws `IllegalArgumentException` if absent.
3. `reader = new HistoryReader(history)` (defensive `List.copyOf`); `reader.next()` consumes `WorkflowStarted`. `workflowStartTime = started.recordedAt()`.
4. `collector = new CommandCollector()`; `ctx = new WorkflowContextImpl(workflowId, version, reader, collector, workflowStartTime)` — signature matches impl, no `DeterministicClock/Random` args (early chat sketch passing those was superseded).
5. `workflow.run(ctx)` in try/catch:
   - `WorkflowSuspended` (an `Error`, stackless, `super(msg,null,false,false)`) -> `classify(collector.snapshot())`
   - `NonDeterminismException` (`extends RuntimeException`, not `Error` — early sketch suggesting `extends Error` was rejected) -> `Nondeterministic(message)`
   - `Exception` -> `Failed(exception-class, message-or-default)`
   - normal return -> `Failed("WorkflowReturned", ...)`
6. `classify()`: empty -> `Failed("EmptySuspension")`; last=`CompleteWorkflow` -> `Completed(result)`; last=`FailWorkflow` -> `Failed(failureType(), message())` (accessor is `failureType()`, not `type()`); else `Suspended(commands)`.

## Matching rules (code truth vs chat intent)

- `HistoryReader`: cursor over `List.copyOf(events)`, `peek()/next()/lastConsumed()/position()/hasUnprocessedHistory()/remaining()/size()`. Single forward cursor, no skipping, no ID index. `hasUnprocessedHistory/remaining` exist but engine ignores them.
- Timer (`WorkflowContextImpl.sleep:126-167`): `timerId = workflowId.toString() + ":timer:" + commandSequence++`. New history (`peek()==null`) -> emit `StartTimer(fireAt=clock.now().plus(duration))` + suspend. `peek() instanceof TimerStarted` -> `next()`, then `peek() instanceof TimerFired`? `next()` + return : suspend. Else (`ActivityScheduled`, `WorkflowCompleted`, etc. where timer expected) -> `NonDeterminismException("Expected TimerStarted but found X")`. **No `timerId` or `fireAt` comparison** — any `TimerStarted` satisfies any `sleep()`. Chat TODO to validate IDs is open.
- Activity: **unimplemented**. `executeActivity()` throws before touching history/collector. Required future contract (from chats): peek `ActivityScheduled(name,id)` then `ActivityCompleted(id,result)`/`ActivityFailed(...)`; name/ID mismatch -> nondeterminism; scheduled-but-incomplete -> suspend; absent -> `ScheduleActivity` + suspend. None of this runs today.
- Clock: `DeterministicClock.now()` = `lastConsumed.recordedAt()`, else start. Advances only as `reader.next()` consumed (timers consume 1-2 events per `sleep()`).
- Random: `DeterministicRandom` seeded from `workflowId.toString()` (record toString), SplitMix64; `nextDouble/nextUUID` call-order stable, history-independent. No `SideEffectRecorded` — chat side-effect design not implemented, so extra `random()` calls shift stream silently.
- `commandSequence`: single counter, currently incremented only by `sleep()`. Future activities must share it; no protocol enforces this yet.

## Nondeterminism examples (current detection capability)

- `sleep()` where history has `ActivityScheduled` -> `NonDeterminismException` (type mismatch — works today).
- Activity order swap -> **not detected today** (throws `UnsupportedOperationException` before matching).
- Changed `sleep()` count/order -> detected only as type mismatch, not ID mismatch (IDs ignored).
- Changed `version` -> ignored (stored, never branched).
- Shortened workflow completing early with trailing history -> **not flagged** (leftover check deliberately omitted).

Related: [[decisions/ADR-001 - Workflow Context Boundary]], [[components/B - Workflow Context]].
