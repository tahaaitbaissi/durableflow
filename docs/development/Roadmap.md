# Roadmap

Parent: [[development/Current Status]]. Plan items M0-M9 below summarize the roadmap chats (Postgres, workers, starter, chaos) — only M1-M2 have code; M3+ is plan, not progress.

## M1 — Command and event model (broken, not done)

- [ ] `Command` / `HistoryEvent` sealed hierarchies compile (blocked: `CancelWorkflow` / `WorkflowCanceled` missing).
- [x] Record shapes for commands/events/values present with correct accessors (`result()`, `failureType()`).
- [ ] `Payload` serialization decision (`toString()` today; JSON/`toObject` proposed, absent).
- [ ] `ActivityOptions.defaults()` decision (chat examples use it, code lacks it).

## M2 — Workflow Context and Replay Engine (current, partial)

- [x] `WorkflowContext` API, `HistoryReader` (full cursor API), `CommandCollector`.
- [x] `DeterministicClock` (cursor-aware), `DeterministicRandom` (seeded PRNG — recorded side-effects deferred).
- [x] Timer `sleep()` type-order match-or-suspend (ID/fireAt validation deferred).
- [x] `Workflow` (`void run`), `WorkflowRegistry` (`HashMap`), `ReplayEngine` + `ReplayResult` with matching ctors/accessors.
- [ ] Activity `executeActivity` + stub integration (throws today).
- [ ] `mvn compile` green (blocked P0).
- [ ] Replay integration tests (activities blocked, timers/clock/random/terminal testable now — see [[development/Testing Strategy]]).

## M3 — Storage and worker (not started, design from chats)

- In-memory `EventStore` + `TaskQueue` ports first (contract tests), then Postgres (`JdbcClient`, Flyway, `SELECT ... FOR UPDATE SKIP LOCKED`, partial index, one-transaction claim).
- `CommandApplier` (`Suspended -> events`, idempotent re-apply) + fencing `version`, leases/heartbeats/reaper.
- Fake activity executor + timer firer for end-to-end test.
- See [[components/E - Storage]].

## M4 — Hardening (not started)

- Versioning: `version` passed through today, never branched; patch markers vs pinned version (chats lean pinned version for v0.1).
- Retry/timeout enforcement using `ActivityOptions`/`RetryPolicy` (records exist, unenforced).
- Cancel path end-to-end (needs P0 fix + `ReplayResult` decision).
- `SideEffectRecorded` for random if seeded-PRNG drift unacceptable.
- `Main` demo or removal.

## M5+ (chat plan: starter, chaos, docs — not started)

- `flow-spring-boot-starter` (auto-config, `@ConfigurationProperties flow.*`, bean scanning), sample app, Actuator/Micrometer.
- Chaos suite (kill-9, Testcontainers, Toxiproxy), invariants, benchmarks, README/ADRs, publish.

## Next 5 concrete tasks (code order)

1. Fix `permits` gaps -> `compile` green.
2. Implement `executeActivity` mirroring `sleep()` + ID/name policy (ADR-003 update).
3. Add JUnit5 to `pom.xml`, create `src/test`, write timer/clock/random/terminal tests first (activities after 2).
4. Decide `Payload` + `ActivityOptions.defaults()` + `toString/value` seed + zero-sleep + null-complete policies.
5. Update [[development/Current Status]] from test results; only then start M3 ports.
