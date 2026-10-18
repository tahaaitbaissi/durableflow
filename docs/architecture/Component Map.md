# Component Map

Parent: [[architecture/System Overview]]

| Concept | Code (verified) | Docs |
|---|---|---|
| Commands | `flowcore/commands/Command.java:5-10` (permits 5, 4 files), `ScheduleActivity.java`, `StartTimer.java`, `CompleteWorkflow.java`, `FailWorkflow.java` (`CancelWorkflow.java` missing) | [[components/A - Commands and Events]] |
| Events / history | `flowcore/events/HistoryEvent.java:7-16` (permits 9, 8 files), `WorkflowStarted.java`, `ActivityScheduled.java`, `ActivityCompleted.java`, `ActivityFailed.java`, `TimerStarted.java`, `TimerFired.java`, `WorkflowCompleted.java`, `WorkflowFailed.java` (`WorkflowCanceled.java` missing) | [[components/A - Commands and Events]] |
| Workflow identity | `flowcore/WorkflowInstance.java`, `flowcore/WorkflowStatus.java`, `flowcore/value/WorkflowId.java`, `WorkflowType.java`, `ActivityId.java`, `TimerId.java`, `Payload.java` | [[architecture/Workflow Lifecycle]] |
| Context API + impl | `flowcore/context/WorkflowContext.java`, `WorkflowContextImpl.java:33-58` ctor `(WorkflowId, version, HistoryReader, CommandCollector, Instant)` | [[components/B - Workflow Context]] |
| History cursor + collector | `flowcore/context/HistoryReader.java` (`peek/next/lastConsumed/position/hasUnprocessedHistory/remaining/size`), `CommandCollector.java` | [[components/B - Workflow Context]] |
| Control flow | `flowcore/context/WorkflowSuspended.java` (`extends Error`), `NonDeterminismException.java` (`extends RuntimeException`) | [[components/B - Workflow Context]], [[architecture/Deterministic Replay]] |
| Activity stubs | `flowcore/activity/ActivityStubFactory.java`, `ActivityInvoker.java` + `flowcore/config/ActivityOptions.java`, `RetryPolicy.java` (no `defaults()`) | [[components/C - Activity Stubs]] |
| Replay | `flowcore/replay/ReplayEngine.java`, `ReplayResult.java` + `flowcore/workflow/Workflow.java`, `WorkflowRegistry.java` (`HashMap`, not concurrent) | [[components/D - Replay Engine]] |
| Time / random | `flowcore/context/DeterministicClock.java` (cursor-aware), `DeterministicRandom.java` (seed = `workflowId.toString()`) — no `SideEffectRecorded` | [[components/F - Time and Randomness]] |
| Storage/worker (absent) | no `store/`/`worker/` package, no Flyway/JdbcClient, no `src/test` | [[components/E - Storage]] |
| Entrypoint (stub) | `org/example/Main.java:4` (`static void main()` — no args, not runnable) | [[development/Roadmap]] |

Package root: `src/main/java/org/example/flowcore/`. Proposed-but-absent (chats only): `SideEffectRecorded`, `RecordSideEffect`, `ActivityFailureException`, `CommandApplier`, `WorkflowTaskWorker`.
