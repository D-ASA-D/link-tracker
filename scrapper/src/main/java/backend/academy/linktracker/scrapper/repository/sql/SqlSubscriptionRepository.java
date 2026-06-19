package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.sql.mapper.SubscriptionRowMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlSubscriptionRepository implements SubscriptionRepository {

    private static final SubscriptionRowMapper SUBSCRIPTION_ROW_MAPPER = SubscriptionRowMapper.INSTANCE;

    private final JdbcClient jdbcClient;

    @Override
    public SubscriptionRecord save(Long chatId, Long linkId) {
        try {
            Long id = jdbcClient
                    .sql("""
                    INSERT INTO subscriptions(chat_id, link_id)
                    VALUES (:chatId, :linkId)
                    RETURNING id
                    """)
                    .param("chatId", chatId)
                    .param("linkId", linkId)
                    .query(Long.class)
                    .single();

            return new SubscriptionRecord(id, chatId, linkId);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Subscription already exists");
        }
    }

    @Override
    public Optional<SubscriptionRecord> findByChatIdAndLinkId(Long chatId, Long linkId) {
        return jdbcClient
                .sql("""
                SELECT id, chat_id, link_id
                FROM subscriptions
                WHERE chat_id = :chatId AND link_id = :linkId
                """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .optional();
    }

    @Override
    public List<SubscriptionRecord> findByChatId(Long chatId, int limit, int offset) {
        return jdbcClient
                .sql("""
                SELECT id, chat_id, link_id
                FROM subscriptions
                WHERE chat_id = :chatId
                ORDER BY id
                LIMIT :limit OFFSET :offset
                """)
                .param("chatId", chatId)
                .param("limit", limit)
                .param("offset", offset)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .list();
    }

    @Override
    public List<SubscriptionRecord> findByLinkId(Long linkId) {
        return jdbcClient
                .sql("""
                SELECT id, chat_id, link_id
                FROM subscriptions
                WHERE link_id = :linkId
                ORDER BY id
                """)
                .param("linkId", linkId)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .list();
    }

    @Override
    public boolean existsByChatIdAndLinkId(Long chatId, Long linkId) {
        return Boolean.TRUE.equals(jdbcClient
                .sql("""
                    SELECT EXISTS(
                        SELECT 1
                        FROM subscriptions
                        WHERE chat_id = :chatId AND link_id = :linkId
                    )
                    """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(Boolean.class)
                .single());
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql("""
                DELETE FROM subscriptions
                WHERE id = :id
                """).param("id", id).update();
    }

    @Override
    public long countByLinkId(Long linkId) {
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM subscriptions
                WHERE link_id = :linkId
                """).param("linkId", linkId).query(Long.class).single();
    }

    @Override
    public List<SubscriptionRecord> findByChatIdAndTag(Long chatId, String tagName) {
        return jdbcClient
                .sql("""
                SELECT s.id, s.chat_id, s.link_id
                FROM subscriptions s
                JOIN subscription_tags st ON st.subscription_id = s.id
                JOIN tags t ON t.id = st.tag_id
                WHERE s.chat_id = :chatId
                  AND t.name = :tagName
                ORDER BY s.id
                """)
                .param("chatId", chatId)
                .param("tagName", tagName)
                .query(SUBSCRIPTION_ROW_MAPPER)
                .list();
    }
}
