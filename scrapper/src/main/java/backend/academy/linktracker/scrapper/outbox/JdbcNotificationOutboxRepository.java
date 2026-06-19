package backend.academy.linktracker.scrapper.outbox;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Primary
@Repository
@RequiredArgsConstructor
public class JdbcNotificationOutboxRepository implements NotificationOutboxRepository {

    private static final String EVENT_TYPE = "LINK_UPDATE";
    private static final String DATABASE_SCOPE = "database";
    private static final String OUTBOX_SCOPE_TYPE = "notification_outbox";

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final ScrapperMetricsService metricsService;

    @Override
    public void save(RawLinkUpdate update) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, OUTBOX_SCOPE_TYPE, () -> saveInternal(update));
    }

    private void saveInternal(RawLinkUpdate update) {
        UUID id = UUID.randomUUID();
        String payload = toJson(update);

        jdbcClient
                .sql("""
            INSERT INTO notification_outbox (
                id,
                aggregate_id,
                event_type,
                payload,
                status,
                attempts,
                created_at,
                updated_at
            )
            VALUES (
                :id,
                :aggregateId,
                :eventType,
                CAST(:payload AS jsonb),
                :status,
                0,
                now(),
                now()
            )
            """)
                .param("id", id)
                .param("aggregateId", update.id())
                .param("eventType", EVENT_TYPE)
                .param("payload", payload)
                .param("status", OutboxEventStatus.NEW.name())
                .update();
    }

    @Override
    public List<OutboxEvent> takeNewBatchForProcessing(int limit) {
        return metricsService.recordRequestDuration(DATABASE_SCOPE, OUTBOX_SCOPE_TYPE, () -> jdbcClient
                .sql("""
            UPDATE notification_outbox
            SET status = :processingStatus,
                updated_at = now()
            WHERE id IN (
                SELECT id
                FROM notification_outbox
                WHERE status = :newStatus
                ORDER BY created_at ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING
                id,
                aggregate_id,
                event_type,
                payload::text AS payload,
                status,
                attempts,
                last_error,
                created_at,
                updated_at,
                sent_at
            """)
                .param("processingStatus", OutboxEventStatus.PROCESSING.name())
                .param("newStatus", OutboxEventStatus.NEW.name())
                .param("limit", limit)
                .query((rs, ignored) -> map(rs))
                .list());
    }

    @Override
    public void markSent(UUID id) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, OUTBOX_SCOPE_TYPE, () -> jdbcClient
                .sql("""
                UPDATE notification_outbox
                SET status = :status,
                    sent_at = now(),
                    updated_at = now(),
                    last_error = NULL
                WHERE id = :id
                """)
                .param("id", id)
                .param("status", OutboxEventStatus.SENT.name())
                .update());
    }

    @Override
    public void markFailedAttempt(UUID id, int maxAttempts, String error) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, OUTBOX_SCOPE_TYPE, () -> jdbcClient
                .sql("""
            UPDATE notification_outbox
            SET attempts = attempts + 1,
                status = CASE
                    WHEN attempts + 1 >= :maxAttempts THEN :failedStatus
                    ELSE :newStatus
                END,
                last_error = :error,
                updated_at = now()
            WHERE id = :id
            """)
                .param("id", id)
                .param("maxAttempts", maxAttempts)
                .param("failedStatus", OutboxEventStatus.FAILED.name())
                .param("newStatus", OutboxEventStatus.NEW.name())
                .param("error", error)
                .update());
    }

    private OutboxEvent map(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getLong("aggregate_id"),
                rs.getString("event_type"),
                rs.getString("payload"),
                OutboxEventStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getString("last_error"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("sent_at") == null
                        ? null
                        : rs.getTimestamp("sent_at").toInstant());
    }

    private String toJson(RawLinkUpdate update) {
        try {
            return objectMapper.writeValueAsString(update);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize raw link update", e);
        }
    }
}
