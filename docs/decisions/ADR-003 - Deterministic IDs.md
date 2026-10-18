# ADR-003 — Deterministic IDs

- Status: Proposed
- Date: 2026-10-18

## Context

Replay matching needs stable correlation between commands and events. Code today: `timerId(seq) = new TimerId(workflowId.toString() + ":timer:" + seq)` (`WorkflowContextImpl.java:169-173`), `seq = commandSequence++` in `sleep()` only. `executeActivity()` throws before consuming a sequence number, so no `ActivityId` scheme runs. Activity-name scheme in code (not yet used for matching): `activityInterface.getName() + "." + method.getName()` (`WorkflowContextImpl.java:77-78`).

Seed note: `DeterministicRandom` also uses `workflowId.toString()` (record toString `"WorkflowId[value=...]"`), not `value()` — same coupling concern.

## Decision (proposed, timer-only implemented)

Derive timer IDs deterministically from `(workflowId.toString(), call-sequence)` by default. Activity-ID policy undecided — options are same sequence scheme vs caller-supplied `ActivityId`.

Timer precedent (intended, not enforced): `StartTimer.timerId` should equal consumed `TimerStarted/TimerFired.timerId`. Current `sleep()` ignores IDs (type-only check), so this is design intent, not code behavior.

## Alternatives considered

### Caller-supplied IDs only

Rejected as default: easy to generate unstable IDs (e.g. `UUID.randomUUID()`), breaking replay. May allow as override for idempotency across code changes.

### Fully random server-assigned IDs

Rejected: replay could not correlate without extra mapping table.

### Semantic activity names only (no sequence IDs)

Partially present as `iface.getName()+"."+method.getName()` string, but overloads collide and reordering breaks. Needs sequence or param-type qualifier if kept.

## Consequences

Positive:

- Stable across replay without external coordination (timers demonstrate the shape).
- Simple to implement and test.

Negative:

- Single shared `commandSequence` means inserting a new activity/timer call shifts later sequences -> history mismatch. Needs `version` / patch mechanism (see [[development/Roadmap]] M4). `version` exists on `WorkflowInstance`/`WorkflowContext` but is never branched on.
- `toString()`-based seeding/IDs couple streams to record-`toString` format; prefer `value()` if `WorkflowId` ever changes.
- Debuggability slightly worse than semantic names.

## Related notes

- [[components/C - Activity Stubs]], [[components/F - Time and Randomness]], [[research/Deterministic Replay]], [[components/B - Workflow Context]]
