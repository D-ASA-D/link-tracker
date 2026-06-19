package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.model.ProcessedKafkaMessageKey;
import backend.academy.linktracker.bot.repository.ProcessedKafkaMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessedKafkaMessageService {

    private final ProcessedKafkaMessageRepository repository;

    @Transactional(readOnly = true)
    public boolean isProcessed(String topic, int partition, long offset) {
        return repository.exists(new ProcessedKafkaMessageKey(topic, partition, offset));
    }

    @Transactional
    public void markProcessed(String topic, int partition, long offset, String messageKey) {
        repository.save(new ProcessedKafkaMessageKey(topic, partition, offset), messageKey);
    }
}
