---
type: component
status: implemented-unverified
milestone: M2
---

# Component F — Time and Randomness

## Purpose

Make `now()`, timers, and random values deterministic across replay. Workflow code must never call wall-clock or PRNG directly.

## Responsibilities

- `DeterministicClock(HistoryReader history, Instant workflowStartTime)` (`context/DeterministicClock.java:13-30`): `now()` = `history.lastConsumed().recordedAt()` if any event consumed, else `workflowStartTime`. Never calls `Instant.now()`. Cursor-aware (reads replay position, not end of full list).
- `DeterministicRandom(String workflowId)` (`context/DeterministicRandom.java:10-54`): FNV-1a64 `hashSeed`, SplitMix64 `nextLong()`, `nextDouble() = (nextLong()>>>11)*0x1.0p-53`, `nextUUID()` with v4 + RFC4122 variant bits. Pure in-memory, consumes **no** history events.
- Wiring: `WorkflowContextImpl` seeds with `workflowId.toString()` (`WorkflowContextImpl.java:46-48`), **not** `workflowId.value()`. For a record `WorkflowId(String value)`, `toString()` is `"WorkflowId[value=...]"`, not the raw id. Stable today, but coupled to record-`toString` format; changing `WorkflowId` shape changes every random stream.
- `WorkflowContextImpl.sleep(Duration)` (`WorkflowContextImpl.java:126-167`): `requireNonNull`, reject negative (`IllegalArgumentException`), allow zero; `seq = commandSequence++`; `fireAt = clock.now().plus(duration)`; type-order match on `TimerStarted`/`TimerFired` (no ID/fireAt comparison — see below).

## Non-responsibilities

- No wall-clock reads, no `Thread.sleep`, no real `UUID.randomUUID()` inside workflow path. No `SideEffectRecorded` persistence.

## Dependencies

- `HistoryReader.lastConsumed()`, `WorkflowId`.

## Inputs / Outputs

- Inputs: history cursor + start `Instant` + call order + `workflowId` string.
- Outputs: `Instant`, `double`, `UUID` stable for same `workflowId.toString()` + same call order + same consumed prefix.

## Invariants

- Same consumed prefix + same call sequence -> same `now()`/random stream.
- Random stream depends only on seed string + call count, **not** on history content or wall time. Consequence: inserting an extra `random()` call shifts all later values; two different histories with same call count yield same randoms. This is the seeded-PRNG trade-off chats explicitly chose as temporary; recorded side-effects (`SideEffectRecorded`/`RecordSideEffect` in chat sketches) are **not implemented**.
- Workflow code must use `ctx.now()/random*()` exclusively; direct `Instant.now()`, `Math.random()`, `UUID.randomUUID()` are determinism bugs (unenforced, no ArchUnit/Checkstyle yet).
- `DeterministicRandom` is not cryptographically secure; never use for tokens/credentials (noted in chats).

## Implementation status

Implemented, unverified. Code matches cursor-aware clock proposal and seeded-PRNG proposal from chats. No tests. `sleep()` is the reference for match-or-suspend but only checks event **types in order**, not `timerId` equality or `fireAt` equality. Prior docs claiming "same timerId -> consume" overstated the check — current code ignores IDs entirely.

## Known issues

- `workflowId.toString()` vs `workflowId.value()` seed: confirm intended; prefer `value()` for stability if `WorkflowId` ever gains fields.
- `sleep()` should eventually validate `timerId` and `fireAt` per chat TODO; currently any `TimerStarted` satisfies any `sleep()` call at that cursor position.
- Zero-duration sleep allowed; confirm intended vs reject.
- Random has no history record, so workflow-code insertion shifts stream with no `NonDeterminismException` — document as temporary (chats plan recorded side-effects later).
- `Duration.plus` uses `clock.now()` at call time; verify this equals persisted `TimerStarted.fireAt` on re-replay (clocks diverge if history grew between calls — by design, but needs test).

## Tests

- [ ] No events consumed -> `now()` == `workflowStartTime` (from `WorkflowStarted.recordedAt()`).
- [ ] After `reader.next()` -> `now()` == last consumed `recordedAt`.
- [ ] Same history -> same `now()` sequence on re-replay.
- [ ] `sleep` + fire round-trip preserves `fireAt` across replays.
- [ ] Same seed string + same call order -> same `randomUUID` stream; different ids diverge; `nextDouble` in `[0,1)`.
- [ ] Extra `random()` insertion shifts later values (document, not fail — until side-effects land).

## Related notes

- [[components/B - Workflow Context]], [[architecture/Deterministic Replay]], [[decisions/ADR-001 - Workflow Context Boundary]]
