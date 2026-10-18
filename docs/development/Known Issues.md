# Known Issues

Source of truth for reproducible breaks. Canonical status lives in [[development/Current Status]]. All paths verified against current `flowcore/` sources.

## P0 — Compile breakers

1. `commands/Command.java:5-10` permits `CancelWorkflow` but `CancelWorkflow.java` does not exist (glob confirms absent) -> `mvn compile` fails.
   - Fix: create `record CancelWorkflow(WorkflowId workflowId, ...)` + handling, or remove from `permits`. Shape never defined in chats — do not invent fields silently.
2. `events/HistoryEvent.java:7-16` permits `WorkflowCanceled` but `WorkflowCanceled.java` does not exist -> same failure.
   - Fix: create `record WorkflowCanceled(WorkflowId, long sequence, Instant recordedAt, ...)` + `ReplayResult.Canceled` or equivalent, or remove from `permits`.

## P1 — Blocking M2

3. `context/WorkflowContextImpl.java:71-86` `executeActivity()` always throws `UnsupportedOperationException`. Repro: call any stub method inside `Workflow.run` -> immediate throw, no history inspection. No `ScheduleActivity` emitted.
4. `sleep()` (`WorkflowContextImpl.java:126-167`) validates types only, not `timerId`/`fireAt`. Repro: history `TimerStarted(id=A)` satisfies `sleep()` expecting `id=B` — incorrectly returns/suspends instead of `NonDeterminismException`. Chat TODO open.
5. `Main.java:4` is `static void main()` with no `String[] args` — not a JVM entrypoint, body empty. Fix or delete.
6. `Payload.from(Object)` (`value/Payload.java:4-6`) is `new Payload(object.toString())` — NPE on `null`, lossy for objects, no JSON. Chat `toObject()` sketches do not exist.
7. `ActivityOptions` has no `defaults()`; chat examples calling `ActivityOptions.defaults()` do not compile. No validation on timeouts/retry.
8. No `src/test`, no JUnit in `pom.xml` (`pom.xml:11-15` only sets Java 27 + encoding) — nothing verified.

## P2 — Design gaps / drift risks

9. `ReplayResult` has no `Canceled` variant though `WorkflowStatus.CANCELED` exists.
10. `RetryPolicy.nextDelay` uncalled; `ActivityOptions`/`RetryPolicy` validation missing (`null`, negative, `maxAttempts<1` accepted at construction; only `nextDelay(attempt<1)` guards).
11. `ActivityStubFactory` rejects default methods; overloads collide (`iface.getName()+"."+method.getName()` ignores params). Document or fix.
12. `DeterministicRandom` seeded from `workflowId.toString()` (`"WorkflowId[value=...]"`), not `value()`. Stable but brittle; changing `WorkflowId` shape shifts all streams.
13. Trailing unconsumed history ignored by design today (`hasUnprocessedHistory/remaining` exist, engine never checks). Revisit only with event-protocol policy — not a bug yet.
14. `WorkflowRegistry` is `HashMap`, not thread-safe; concurrent register+replay unsafe. Register-before-run only.
15. `complete(null)` NPEs inside `Payload.from`; zero-duration `sleep()` allowed (only `null`/negative rejected). Confirm intended.

## Repro (code-level, no mvn in this env)

```powershell
# P0: glob shows no CancelWorkflow.java / WorkflowCanceled.java
# yet Command.java / HistoryEvent.java permit them
Get-ChildItem -Recurse src\main\java\*.java | Select-String "CancelWorkflow|WorkflowCanceled"
# P1: any stub call
# workflow.run(ctx) with ctx.newActivityStub(...).someMethod() -> UnsupportedOperationException
```
