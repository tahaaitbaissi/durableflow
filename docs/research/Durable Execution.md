# Durable Execution

Research question: how do workflow engines suspend/resume long-running code without losing progress?

## In own words

Durable execution persists every workflow decision as history. On resume, code re-runs from the start but short-circuits completed external calls with recorded results, then blocks (suspends) at the first incomplete call.

## Why it matters here

This is exactly `ReplayEngine.replay()` + `WorkflowSuspended`: workflow code (`Workflow.run(ctx)`) is pure decision logic; durability comes from history, not from keeping threads alive. Code truth: timers replay type-only, activities blocked, no signals/saga/workers yet — see [[development/Current Status]].

## How established systems do it

- Temporal: workflow task = replay history → produce commands → persist → schedule activities/timers. See [[research/Sources]].
- Key constraint everywhere: workflow code must be deterministic; nondeterministic APIs banned or virtualized.

## Design options

1. Re-execute from start each task (chosen — simple, matches `WorkflowContextImpl` fresh-per-replay).
2. Snapshot continuations/threads (complex, JVM-unfriendly).
3. Step-function state machines (less expressive than code).

## Choice + implications

Option 1. Implies: fresh context per replay, `WorkflowSuspended` as control flow, deterministic `now/random`, stable IDs (ADR-003).

## To test

- Re-replay same history twice → identical commands.
- Long chain (N activities) resumes from tail without re-executing heads.

Related: [[architecture/Deterministic Replay]], [[decisions/ADR-002 - Replay and Persistence Separation]]
