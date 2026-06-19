package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository {

    void save(RawLinkUpdate update);

    List<OutboxEvent> takeNewBatchForProcessing(int limit);

    void markSent(UUID id);

    void markFailedAttempt(UUID id, int maxAttempts, String error);
}
