package backend.academy.linktracker.scrapper.outbox;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxTransactionService {

    private final NotificationOutboxRepository outboxRepository;

    @Transactional
    public List<OutboxEvent> takeNewBatch(int limit) {
        return outboxRepository.takeNewBatchForProcessing(limit);
    }

    @Transactional
    public void markSent(UUID id) {
        outboxRepository.markSent(id);
    }

    @Transactional
    public void markFailedAttempt(UUID id, int maxAttempts, String error) {
        outboxRepository.markFailedAttempt(id, maxAttempts, error);
    }
}
