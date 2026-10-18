# Sources

## Deterministic workflow replay

### Temporal — Workflow concepts

- URL: https://docs.temporal.io/workflows
- Accessed: 2026-10-18
- Type: Official docs
- Concepts: workflow definitions, event history, deterministic replay
- Used in: [[research/Deterministic Replay]], [[architecture/System Overview]]
- Notes: Recorded activity results reused; no re-execution on replay.

### Temporal — Event history

- URL: https://docs.temporal.io/encyclopedia/event-history
- Accessed: 2026-10-18
- Type: Official docs
- Concepts: commands vs events, history ordering
- Used in: [[architecture/System Overview]], [[research/Event Sourcing]]
- Notes: Command/event correspondence model for `System Overview`.

### Java — language / API docs

- URL: https://docs.oracle.com/en/java/
- Accessed: 2026-10-18
- Type: Official docs
- Concepts: reflection, dynamic proxies, records, time APIs
- Used in: [[components/C - Activity Stubs]], [[components/A - Commands and Events]]

### Maven — build guides

- URL: https://maven.apache.org/guides/
- Accessed: 2026-10-18
- Type: Official docs
- Concepts: build lifecycle, compiler, test execution
- Used in: [[development/Testing Strategy]]

## Research rules

- Record URL + access date for key sources.
- Prefer official docs for API behavior.
- Label assumptions vs verified facts.
- Record disagreements; link each major decision to the research that influenced it.
