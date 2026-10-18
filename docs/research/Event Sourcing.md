# Event Sourcing

Research question: what is the command vs event distinction, and how should history be shaped?

## In own words

Commands are requests from decision logic ("schedule activity X"); events are facts ("activity X completed with Y"). Replay produces commands; storage turns commands into events; future replays read events.

## Why it matters here

`commands/*` vs `events/*` split already encodes this, but mapping must stay 1:1 and versioned. Confusing them (e.g. letting `WorkflowContext` write events) would couple replay to storage. Code truth: `ScheduleActivity/StartTimer/CompleteWorkflow/FailWorkflow` shapes exist; no `CommandApplier`/worker translates them yet; `ActivityFailed.retryable` exists but no retry loop reads it.

## How established systems do it

- Temporal event history: `ActivityTaskScheduled/Started/Completed/Failed`, `TimerStarted/Fired`, `WorkflowExecutionCompleted/Failed/Canceled`. Our `HistoryEvent` mirrors a subset.
- Rules: append-only, ordered `sequence`, immutable payloads.

## Design options

1. Fine-grained events (per attempt) vs coarse (per activity). Current: coarse + `retryable` flag on `ActivityFailed`.
2. Store commands directly as events vs translate. Current intent: translate (`ScheduleActivity→ActivityScheduled`).

## Choice + implications

Keep coarse events; worker owns translation (ADR-002). Implies idempotent applier + monotonic `sequence` assignment in [[components/E - Storage]].

## To test

- Command→event translation table.
- Re-applied `Suspended` does not duplicate events.

Related: [[components/A - Commands and Events]], [[components/E - Storage]]
