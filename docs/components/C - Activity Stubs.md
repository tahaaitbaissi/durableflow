---
type: component
status: scaffold-only
milestone: M2
---

# Component C — Activity Stubs

## Purpose

Type-safe proxy so workflow code calls `myActivities.doWork(args)` instead of hand-building commands.

## Responsibilities

- `ActivityStubFactory(ActivityInvoker)` (`activity/ActivityStubFactory.java:11-80`): `create(Class<T>, ActivityOptions)` validates `isInterface()`, builds JDK `Proxy`. `Object` methods handled explicitly (`toString/hashCode/equals`), default methods rejected, all else -> `invoker.invoke(iface, method, args-or-empty, options)`.
- `ActivityInvoker` (`activity/ActivityInvoker.java:8-16`): `@FunctionalInterface invoke(Class<?> iface, Method method, Object[] args, ActivityOptions options) -> Object`.
- Wiring: `WorkflowContextImpl` constructs factory with `this::executeActivity` (`WorkflowContextImpl.java:55-57`); `newActivityStub()` delegates to `factory.create()` (`WorkflowContextImpl.java:61-69`).
- `config/ActivityOptions(startToCloseTimeout, heartbeatTimeout, retryPolicy)` and `RetryPolicy(maxAttempts, initialInterval, backoffCoefficient, maxInterval, nonRetryableExceptionTypes:Set<String>)` + `nextDelay(attempt)` exponential-capped.

## Non-responsibilities

- No network, no execution, no retry loop, no timeout enforcement.

## Dependencies

- Bound invoker is `WorkflowContextImpl::executeActivity` (currently throws `UnsupportedOperationException`, `WorkflowContextImpl.java:82-85`).
- `commands/ScheduleActivity`, `events/ActivityScheduled/Completed/Failed` (shapes exist, matching logic does not).

## Inputs / Outputs

- Inputs: activity interface + method call + options.
- Intended outputs (not yet): recorded `Payload` result, or `ScheduleActivity` command + suspend, or thrown activity failure.
- Actual output today: always throws `UnsupportedOperationException("Activity execution is not implemented yet: " + activityName)`.

## Invariants

- Stub calls must be deterministic: same call order + same history -> same result (unenforced until `executeActivity` lands).
- Current activity-name scheme in code: `activityInterface.getName() + "." + method.getName()` (`WorkflowContextImpl.java:77-78`). Fully-qualified interface + simple method name. Overloads collide (same name, different params) — noted in chats, unresolved. No sequence-derived `ActivityId` yet.

## Implementation status

Scaffold only. Factory + proxy mechanics match the cleaner chat proposal (separate factory, `ActivityInvoker` seam, safe `Object` handling) — this part **is** implemented. End-to-end path dead-ends at `executeActivity`. `RetryPolicy.nextDelay` math exists but never called; timeouts/retries unenforced. `ActivityOptions` has no `defaults()` — chat examples calling `ActivityOptions.defaults()` do not compile.

Package note: factory lives in `flowcore/activity/`, not `flowcore/context/` as some early chat sketches showed. Current location is correct.

## Known issues

- Implement `executeActivity`: peek `ActivityScheduled` + `ActivityCompleted/Failed`, compare `activityName`/`ActivityId`/input, return result or throw, else emit `ScheduleActivity` + suspend. Must decide ID scheme and whether to validate `ActivityId`/`options`.
- Decide failure mapping: `ActivityFailed(failureType, message, retryable)` -> retry, throw checked/unchecked to workflow, or `NonDeterminismException`? No `ActivityFailureException` exists despite chat sketches.
- Decide overload handling (include param types in activity name?).
- `ActivityOptions`/`RetryPolicy` validation missing (`null` timeouts, `maxAttempts<1`, negative durations accepted at record construction).

## Tests

- [ ] Recorded completion returns result, no new command (blocked).
- [ ] Missing completion -> single `ScheduleActivity` + suspend, no duplicate on re-replay (blocked).
- [ ] Order/name mismatch -> nondeterminism (blocked).
- [ ] `ActivityFailed` -> workflow-visible exception (blocked, no exception type defined).
- [ ] `RetryPolicy.nextDelay` unit values + caps (unblocked — can test now).
- [ ] Factory rejects non-interface, handles `toString/equals/hashCode`, rejects default methods (unblocked).

## Related notes

- [[components/B - Workflow Context]], [[components/D - Replay Engine]], [[decisions/ADR-003 - Deterministic IDs]]
