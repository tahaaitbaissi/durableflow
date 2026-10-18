package org.example.flowcore.context;

import org.example.flowcore.events.HistoryEvent;

import java.util.List;

public final class HistoryReader {

    private final List<HistoryEvent> events;
    private int cursor;

    public HistoryReader(List<HistoryEvent> events) {
        this.events = List.copyOf(events);
        this.cursor = 0;
    }

    public HistoryEvent peek() {
        return cursor < events.size()
                ? events.get(cursor)
                : null;
    }

    public HistoryEvent next() {
        return cursor < events.size()
                ? events.get(cursor++)
                : null;
    }

    public HistoryEvent lastConsumed() {
        return cursor > 0
                ? events.get(cursor - 1)
                : null;
    }

    public int position() {
        return cursor;
    }

    public boolean hasUnprocessedHistory() {
        return cursor < events.size();
    }

    public int remaining() {
        return events.size() - cursor;
    }

    public int size() {
        return events.size();
    }
}
