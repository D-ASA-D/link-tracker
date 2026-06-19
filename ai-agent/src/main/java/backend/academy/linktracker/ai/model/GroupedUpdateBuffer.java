package backend.academy.linktracker.ai.model;

import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class GroupedUpdateBuffer {

    private final Long tgChatId;
    private final Instant createdAt;
    private final List<ProcessedLinkUpdate> updates = new ArrayList<>();

    public GroupedUpdateBuffer(Long tgChatId, Instant createdAt, ProcessedLinkUpdate update) {
        this.tgChatId = tgChatId;
        this.createdAt = createdAt;
        this.updates.add(update);
    }

    public Long tgChatId() {
        return tgChatId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public synchronized void add(ProcessedLinkUpdate update) {
        updates.add(update);
    }

    public synchronized List<ProcessedLinkUpdate> snapshot() {
        return List.copyOf(updates);
    }

    public synchronized int size() {
        return updates.size();
    }
}
