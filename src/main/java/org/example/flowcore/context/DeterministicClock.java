package org.example.flowcore.context;

import org.example.flowcore.events.HistoryEvent;

import java.time.Instant;
import java.util.Objects;

public final class DeterministicClock {

    private final HistoryReader history;
    private final Instant workflowStartTime;

    public DeterministicClock(
            HistoryReader history,
            Instant workflowStartTime) {

        this.history = Objects.requireNonNull(history);
        this.workflowStartTime =
                Objects.requireNonNull(workflowStartTime);
    }

    public Instant now() {
        HistoryEvent latest = history.lastConsumed();

        if (latest == null) {
            return workflowStartTime;
        }

        return latest.recordedAt();
    }
}
