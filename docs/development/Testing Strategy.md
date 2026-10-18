# Testing Strategy

Related: [[development/Current Status]], [[components/D - Replay Engine]]

## State

No tests exist. Verified: no `src/test` directory (glob), `pom.xml:11-15` sets Java 27 + encoding only, no `junit-jupiter` dependency. Add JUnit5 before writing tests.

## Layers (adjusted to code truth)

1. **Unit — pure logic, no replay (all unblocked today).**
   - `DeterministicRandom`: same `workflowId.toString()` seed -> same stream; different IDs diverge; `nextDouble` in `[0,1)`; documents seeded-PRNG shift on insertion (no side-effect record).
   - `RetryPolicy.nextDelay`: exponential + cap, `attempt<1` throws `IllegalArgumentException`.
   - `HistoryReader`/`CommandCollector`: cursor advance, `lastConsumed/position/hasUnprocessed/remaining/size`, `snapshot()` immutability (`List.copyOf`).
   - `DeterministicClock`: no consumption -> start time; after `next()` -> last consumed `recordedAt`.
   - `ActivityStubFactory`: rejects non-interface, handles `toString/equals/hashCode`, rejects default methods, `null args -> empty`.
2. **Replay integration — in-memory history lists.**
   - Timer (testable now, type-only): empty history + `sleep()` -> `Suspended([StartTimer])` with `fireAt=clock.now().plus(d)`; `TimerStarted` only -> suspend no-dup; `Started+Fired` -> return; wrong **type** (e.g. `ActivityScheduled` where timer expected) -> `Nondeterministic`. Note: wrong **ID** does NOT fail today (IDs ignored) — do not assert ID-mismatch until validation lands.
   - Activity (blocked until `executeActivity` lands): recorded `Completed` -> returns result; missing/scheduled-only -> `Suspended(ScheduleActivity)`; `Failed` -> workflow-visible exception (needs exception-type decision); order swap -> `Nondeterministic`. Writing these now forces the implementation.
   - Terminal/guards (testable now): `complete(Object non-null)` -> `Completed(Payload.from(obj))`; `fail` -> `Failed`; return-without-suspend -> `Failed("WorkflowReturned")`; empty suspension -> `Failed("EmptySuspension")`; empty/first-not-started history -> `Failed("InvalidHistory")`; unknown type -> `Failed` (registry `IllegalArgumentException` mapped). `complete(null)` NPEs today — pin behavior explicitly.
3. **Storage/worker/chaos (M3+, future):** contract tests on in-memory vs Postgres, idempotent apply, crash-recovery, kill-9 + Testcontainers + Toxiproxy, invariants (terminal reachability, gap-free sequences, at-least-once + idempotent exactly-once effects, no double-complete).

## Conventions

- One test class per component (`WorkflowContextImplTest`, `ReplayEngineTest`, `DeterministicRandomTest`, ...).
- Build histories with helper `event(sequence, recordedAt, ...)` — explicit sequences, increasing `recordedAt`; always start with `WorkflowStarted`.
- Assert both `ReplayResult` type **and** exact commands emitted (`Suspended.commands()`).
- Mark [[development/Current Status]] items done only when test passes. Chat test lists assuming `ActivityOptions.defaults()` / `toObject()` / side-effects must be adapted — those helpers/types do not exist.

## First tests to write (runnable today, in order)

1. `compile` green after permits fix (prerequisite for all).
2. `HistoryReader`/`CommandCollector`/`DeterministicClock`/`DeterministicRandom`/`RetryPolicy`/`ActivityStubFactory`-mechanics unit tests.
3. Timer suspend + refire + type-mismatch (documents ID-check absence).
4. Terminal mapping + guards (`complete/fail/WorkflowReturned/EmptySuspension/InvalidHistory/unknown-type`).
5. Activity recorded-result (forces `executeActivity` implementation) + order-swap nondeterminism.
6. `fireAt` stability across re-replay (same history -> same `StartTimer.fireAt`).
