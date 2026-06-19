package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.repository.sql.mapper.TagRowMapper;
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
public class SqlTagRepository implements TagRepository {

    private static final TagRowMapper TAG_ROW_MAPPER = TagRowMapper.INSTANCE;

    private final JdbcClient jdbcClient;

    @Override
    public TagRecord save(String name) {
        Long id = jdbcClient.sql("""
                INSERT INTO tags(name)
                VALUES (:name)
                RETURNING id
                """).param("name", name).query(Long.class).single();

        return new TagRecord(id, name);
    }

    @Override
    public Optional<TagRecord> findById(Long id) {
        return jdbcClient.sql("""
                SELECT id, name
                FROM tags
                WHERE id = :id
                """).param("id", id).query(TAG_ROW_MAPPER).optional();
    }

    @Override
    public Optional<TagRecord> findByName(String name) {
        return jdbcClient.sql("""
                SELECT id, name
                FROM tags
                WHERE name = :name
                """).param("name", name).query(TAG_ROW_MAPPER).optional();
    }

    @Override
    public TagRecord findOrCreate(String name) {
        return findByName(name).orElseGet(() -> {
            try {
                return save(name);
            } catch (DuplicateKeyException e) {
                return findByName(name).orElseThrow();
            }
        });
    }

    @Override
    public List<TagRecord> findAll(int limit, int offset) {
        return jdbcClient
                .sql("""
                SELECT id, name
                FROM tags
                ORDER BY id
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", limit)
                .param("offset", offset)
                .query(TAG_ROW_MAPPER)
                .list();
    }

    @Override
    public TagRecord update(Long id, String name) {
        jdbcClient.sql("""
                UPDATE tags
                SET name = :name
                WHERE id = :id
                """).param("id", id).param("name", name).update();

        return findById(id).orElseThrow();
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql("""
                DELETE FROM tags
                WHERE id = :id
                """).param("id", id).update();
    }

    @Override
    public List<TagRecord> findBySubscriptionId(Long subscriptionId) {
        return jdbcClient
                .sql("""
                SELECT t.id, t.name
                FROM tags t
                JOIN subscription_tags st ON st.tag_id = t.id
                WHERE st.subscription_id = :subscriptionId
                ORDER BY t.id
                """)
                .param("subscriptionId", subscriptionId)
                .query(TAG_ROW_MAPPER)
                .list();
    }

    @Override
    public void attachToSubscription(Long subscriptionId, Long tagId) {
        jdbcClient
                .sql("""
                INSERT INTO subscription_tags(subscription_id, tag_id)
                VALUES (:subscriptionId, :tagId)
                ON CONFLICT DO NOTHING
                """)
                .param("subscriptionId", subscriptionId)
                .param("tagId", tagId)
                .update();
    }

    @Override
    public void detachAllFromSubscription(Long subscriptionId) {
        jdbcClient.sql("""
                DELETE FROM subscription_tags
                WHERE subscription_id = :subscriptionId
                """).param("subscriptionId", subscriptionId).update();
    }

    @Override
    public boolean isAttached(Long subscriptionId, Long tagId) {
        Long count = jdbcClient
                .sql("""
                SELECT COUNT(*)
                FROM subscription_tags
                WHERE subscription_id = :subscriptionId
                  AND tag_id = :tagId
                """)
                .param("subscriptionId", subscriptionId)
                .param("tagId", tagId)
                .query(Long.class)
                .single();

        return count != null && count > 0;
    }

    @Override
    public void detachFromSubscription(Long subscriptionId, Long tagId) {
        jdbcClient
                .sql("""
                DELETE FROM subscription_tags
                WHERE subscription_id = :subscriptionId
                  AND tag_id = :tagId
                """)
                .param("subscriptionId", subscriptionId)
                .param("tagId", tagId)
                .update();
    }

    @Override
    public long countUsages(Long tagId) {
        Long count = jdbcClient.sql("""
                SELECT COUNT(*)
                FROM subscription_tags
                WHERE tag_id = :tagId
                """).param("tagId", tagId).query(Long.class).single();

        return count == null ? 0 : count;
    }
}
