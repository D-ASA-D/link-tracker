package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.ProcessedKafkaMessageKey;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka.consumer", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProcessedKafkaMessageRepository {

    private final JdbcClient jdbcClient;

    public boolean exists(ProcessedKafkaMessageKey key) {
        Integer count = jdbcClient
                .sql("""
                SELECT COUNT(*)
                FROM processed_kafka_messages
                WHERE topic = :topic
                  AND partition_id = :partition
                  AND offset_id = :offset
                """)
                .param("topic", key.topic())
                .param("partition", key.partition())
                .param("offset", key.offset())
                .query(Integer.class)
                .single();

        return count != null && count > 0;
    }

    public void save(ProcessedKafkaMessageKey key, String messageKey) {
        jdbcClient
                .sql("""
                INSERT INTO processed_kafka_messages (
                    topic,
                    partition_id,
                    offset_id,
                    message_key,
                    processed_at
                )
                VALUES (
                    :topic,
                    :partition,
                    :offset,
                    :messageKey,
                    now()
                )
                ON CONFLICT (topic, partition_id, offset_id) DO NOTHING
                """)
                .param("topic", key.topic())
                .param("partition", key.partition())
                .param("offset", key.offset())
                .param("messageKey", messageKey)
                .update();
    }
}
