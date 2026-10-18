package org.example.flowcore.context;

import org.example.flowcore.activity.ActivityStubFactory;
import org.example.flowcore.commands.CompleteWorkflow;
import org.example.flowcore.commands.FailWorkflow;
import org.example.flowcore.commands.StartTimer;
import org.example.flowcore.config.ActivityOptions;
import org.example.flowcore.events.HistoryEvent;
import org.example.flowcore.events.TimerFired;
import org.example.flowcore.events.TimerStarted;
import org.example.flowcore.value.Payload;
import org.example.flowcore.value.TimerId;
import org.example.flowcore.value.WorkflowId;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class WorkflowContextImpl implements WorkflowContext {

    private final WorkflowId workflowId;
    private final int version;
    private final HistoryReader history;
    private final CommandCollector commands;
    private final DeterministicClock clock;
    private final DeterministicRandom random;
    private final ActivityStubFactory activityStubFactory;

    private int commandSequence;

    public WorkflowContextImpl(
            WorkflowId workflowId,
            int version,
            HistoryReader history,
            CommandCollector commands,
            Instant workflowStartTime) {

        this.workflowId = workflowId;
        this.version = version;
        this.history = history;
        this.commands = commands;
        this.commandSequence = 0;

        this.random = new DeterministicRandom(
                workflowId.toString()
        );

        this.clock = new DeterministicClock(
                history,
                workflowStartTime
        );

        this.activityStubFactory = new ActivityStubFactory(
                this::executeActivity
        );
    }

    @Override
    public <T> T newActivityStub(
            Class<T> activityInterface,
            ActivityOptions options) {

        return activityStubFactory.create(
                activityInterface,
                options
        );
    }

    private Object executeActivity(
            Class<?> activityInterface,
            Method method,
            Object[] args,
            ActivityOptions options) {

        String activityName =
                activityInterface.getName() + "." + method.getName();

        // Match history, emit ScheduleActivity if needed,
        // and suspend or return the recorded result.
        throw new UnsupportedOperationException(
                "Activity execution is not implemented yet: "
                        + activityName
        );
    }

    @Override
    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void complete(Object result) {
        commands.add(new CompleteWorkflow(workflowId, Payload.from(result)));
        throw new WorkflowSuspended();
    }

    @Override
    public void fail(String type, String message) {
        commands.add(new FailWorkflow(workflowId, type, message));
        throw new WorkflowSuspended();
    }

    @Override
    public Instant now() {
        return clock.now();
    }

    @Override
    public UUID randomUUID() {
        return random.nextUUID();
    }

    @Override
    public double random() {
        return random.nextDouble();
    }

    @Override
    public void sleep(Duration duration) {
        Objects.requireNonNull(duration, "duration");

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                    "Sleep duration cannot be negative"
            );
        }

        int seq = commandSequence++;
        HistoryEvent next = history.peek();

        if (next == null) {
            Instant fireAt = clock.now().plus(duration);

            commands.add(new StartTimer(
                    workflowId,
                    timerId(seq),
                    fireAt
            ));

            throw new WorkflowSuspended();
        }

        if (next instanceof TimerStarted started) {
            history.next();

            HistoryEvent fired = history.peek();

            if (fired instanceof TimerFired) {
                history.next();
                return;
            }

            throw new WorkflowSuspended();
        }

        throw new NonDeterminismException(
                "Expected TimerStarted but found "
                        + next.getClass().getSimpleName()
        );
    }

    private TimerId timerId(int sequence) {
        return new TimerId(
                workflowId.toString() + ":timer:" + sequence
        );
    }
}
