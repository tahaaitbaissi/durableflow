# ADR-001 — Workflow Context Boundary

- Status: Accepted
- Date: 2026-10-18

## Context

Workflow code is re-executed from scratch on every task (`ReplayEngine` creates fresh `WorkflowContextImpl`). Direct calls to wall time, randomness, activity execution, or storage produce different results per replay and corrupt state. Full prevention is impossible in Java (no language ban on `Instant.now()`), so the boundary must be API + docs + tests + review.

## Decision

All workflow side-effect access goes through `WorkflowContext` (`flowcore/context/WorkflowContext.java:10-23`):
`newActivityStub`, `sleep`, `now`, `randomUUID/random`, `getWorkflowId/getVersion`, `complete/fail`.

`WorkflowContextImpl` (`flowcore/context/WorkflowContextImpl.java:21-174`) translates calls to history hits or new commands + `throw WorkflowSuspended`. It never performs I/O. `now()` delegates to cursor-aware `DeterministicClock`; random delegates to seeded `DeterministicRandom` (recorded side-effects deferred — see [[components/F - Time and Randomness]]).

Enforcement stack (from chats, only partially implemented):

- API design — done (context is the only injected capability).
- Docs — done (forbidden list: `Instant.now/System.currentTimeMillis/new Date`, `Math.random/new Random/UUID.randomUUID`, network/file/DB I/O, threads/`CompletableFuture`/`ExecutorService`, static mutable state, env/system properties, `catch(Throwable)` swallowing suspension).
- Tests — planned (replay-twice-compare-commands; see [[development/Testing Strategy]]). None exist yet.
- Code review — process, not code.
- Static analysis (ArchUnit/Checkstyle bans) — proposed, absent.

## Alternatives considered

### Pass `HistoryReader`/`CommandCollector` directly to workflows

Rejected: leaks cursor mechanics, harder to enforce determinism, harder to evolve.

### Allow direct Java calls + record via bytecode

Rejected: too complex for this stage; explicit `ctx` is simpler and testable.

### `NonDeterminismException extends Error` to prevent swallowing

Considered in chats, rejected in code: it extends `RuntimeException` (`NonDeterminismException.java:3-9`). Only `WorkflowSuspended extends Error` (`WorkflowSuspended.java:3-7`). Workflow code must still avoid `catch(Throwable)`/`catch(Error)`.

## Consequences

Positive:

- Single choke point for determinism enforcement.
- Timer `sleep()` demonstrates the match-or-suspend pattern (type-order only for now).
- Testable with in-memory history (no DB needed for `ReplayEngine`).

Negative:

- Workflow authors must learn the `ctx` API; accidental `Instant.now()` is unenforced (no ArchUnit yet).
- `executeActivity` still missing, so activity path is blocked — boundary holds for timers/time/random/terminal only.
- Seeded random without history record means insertion shifts stream silently (no nondeterminism signal until side-effects land).

## Related notes

- [[components/B - Workflow Context]], [[components/F - Time and Randomness]], [[architecture/Deterministic Replay]]
