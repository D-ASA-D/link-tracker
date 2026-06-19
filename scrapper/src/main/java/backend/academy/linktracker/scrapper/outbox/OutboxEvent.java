package backend.academy.linktracker.scrapper.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        Long aggregateId,
        String eventType,
        String payload,
        OutboxEventStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant sentAt) {}
