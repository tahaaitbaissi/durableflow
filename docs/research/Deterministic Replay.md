# Deterministic Replay (Research)

Research question: how is an activity/timer call matched to history, and what counts as nondeterminism?

## In own words

Each deterministic call should consume the next unconsumed event(s). Match on type + ID + (ideally) name/input. Hit -> return recorded outcome. Miss because nothing recorded -> emit command + suspend. Miss because wrong event -> nondeterminism error. Leftover-history handling needs an explicit event-protocol policy; blanket "any trailing event = error" is wrong when future-valid events exist.

## Why it matters here (code truth)

`WorkflowContextImpl.sleep()` implements type-order matching only (no ID/fireAt check); `executeActivity()` implements nothing (throws). `ReplayEngine` distinguishes "not yet done" (suspend -> `Suspended`) from "code changed" (type mismatch -> `NonDeterminismException` -> `Nondeterministic`) only for timers. Activity order-swap, ID-mismatch, and trailing-history cases are currently unhandled. `hasUnprocessedHistory/remaining` exist but are intentionally unchecked.

Chat proposals not in code: `SideEffectRecorded`/`RecordSideEffect` for random, `ActivityFailureException`, blanket leftover-history `Nondeterministic` on suspend/complete. Do not cite these as implemented.

## How established systems do it

- Temporal SDKs assign event IDs / command counters; replay mismatches raise `NonDeterministicException`. Timer/activity IDs correlate commands to completions.
- Time virtualized (history timestamps), random seeded per workflow/task or recorded as side-effect.
- Temporal behavior on nondeterminism: fail the workflow *task* with special code, leave workflow stuck for human fix — not auto-fail workflow. Our chats propose v0.1 simplification (append `WorkflowFailed(type=Nondeterministic)` + log loudly); no code implements either yet.

## Design options

1. Strict positional + type matching (current `sleep()`: `peek/next` only). Simple, brittle to insertion, ignores IDs.
2. Type + ID/name/input validation (intended next step; ADR-003). Catches reordered/renamed calls.
3. ID-keyed lookup (tolerant to reorder, complex, not proposed for v0.1).
4. Version markers / patches (future M4; `version` field exists but unused).

## Choice + implications

Ship (1) for timers now (done), extend to (2) when `executeActivity` lands, add (4) when live version skew matters. Implies single `commandSequence` discipline, `NonDeterminismException` on mismatch at call site, and explicit decision on whether `NonDeterminismException` should ever be catchable by user code (currently `RuntimeException`, so `catch(Exception)` in workflow would swallow it — document + test).

## To test

See [[development/Testing Strategy]] replay cases. Critical: order-swap -> `Nondeterministic`, not silent wrong result (timer type-swap testable now; activity swap blocked). Pin that ID-mismatch is *not* detected today to prevent false confidence.

Related: [[architecture/Deterministic Replay]], [[components/B - Workflow Context]], [[decisions/ADR-001 - Workflow Context Boundary]], [[decisions/ADR-003 - Deterministic IDs]]
