# Session Log

> Running log, newest first. Transfer lasting knowledge to component/decision notes; this file is not the authority.

## 2026-10-18 — Docs accuracy pass (code vs chats)

Goal: fix docs that overstated code; reconcile chat histories (Components B/C/D sketches, M0-M9 roadmap) with 30 files in `src/main/java/org/example/flowcore/`.

Work completed:

- Read all `flowcore/` sources + `Main.java` + `pom.xml`; confirmed: `Command` permits 5/4 present, `HistoryEvent` permits 9/8 present; `sleep()` type-only (no ID/fireAt check); `executeActivity()` throws; `Payload.from=toString()`, no `toObject`; no `ActivityOptions.defaults()`, no `SideEffectRecorded`/`RecordSideEffect`/`ActivityFailureException`/`CommandApplier`/workers/tests; `DeterministicRandom` seeded from `workflowId.toString()`; `WorkflowRegistry=HashMap`; `ReplayEngine` ctor matches `WorkflowContextImpl` ctor; `FailWorkflow.failureType()` correct; `hasUnprocessedHistory` exists but unused (intentional).
- Rewrote `components/A-F`, `architecture/*`, `development/Current Status|Known Issues|Roadmap|Testing Strategy`, `ADR-001/ADR-003`, `research/Deterministic Replay`, `Home` to state code truth and tag chat-only proposals as absent.
- Prior inaccuracies fixed: timer "complete" -> partial; `value()` seed -> `toString()` seed; `type()` -> `failureType()`; `Context`-package factory -> `activity/` package; blanket leftover-check -> deliberately omitted; ctor-mismatch -> resolved; line-number refs updated/removed where brittle.

Decisions made:

- Docs distinguish "code truth" vs "chat proposal" explicitly; proposals stay in docs only as future work.
- Seeded random stays temporary until `SideEffectRecorded` decision; ID validation stays TODO until `executeActivity` lands.

Next session:

1. Fix `permits` gaps, verify `mvn compile`.
2. Implement `executeActivity` (+ ID/name policy, ADR-003 update).
3. Add JUnit5 + `src/test`, write unblocked tests first (clock/random/reader/factory/timer-types/terminal/guards); update [[development/Current Status]].

## 2026-10-18 — Docs vault bootstrap

Goal: replace `src/main/java/org/example/temp.md` with versioned `docs/` Obsidian vault.

Work completed:

- Inventoried `flowcore/`; found P0 breaks; created `docs/` structure.
- Deleted `src/main/java/org/example/temp.md`.

---

Use [[templates/Session Template]] for new entries.
