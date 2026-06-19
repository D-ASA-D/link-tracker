package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.sql.mapper.LinkRowMapper;
import java.sql.Timestamp;
import java.time.Instant;
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
public class SqlLinkRepository implements LinkRepository {

    private static final LinkRowMapper LINK_ROW_MAPPER = LinkRowMapper.INSTANCE;

    private final JdbcClient jdbcClient;

    @Override
    public LinkRecord save(String url, Instant lastUpdated, Instant lastCheckedAt) {
        Instant safeCheckedAt = lastCheckedAt == null ? Instant.now() : lastCheckedAt;

        try {
            Long id = jdbcClient
                    .sql("""
                    INSERT INTO links(url, last_updated, last_checked_at)
                    VALUES (:url, :lastUpdated, :lastCheckedAt)
                    RETURNING id
                    """)
                    .param("url", url)
                    .param("lastUpdated", toTimestamp(lastUpdated))
                    .param("lastCheckedAt", Timestamp.from(safeCheckedAt))
                    .query(Long.class)
                    .single();

            return new LinkRecord(id, url, lastUpdated, safeCheckedAt);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Link already exists");
        }
    }

    @Override
    public Optional<LinkRecord> findByUrl(String url) {
        return jdbcClient.sql("""
                SELECT id, url, last_updated, last_checked_at
                FROM links
                WHERE url = :url
                """).param("url", url).query(LINK_ROW_MAPPER).optional();
    }

    @Override
    public Optional<LinkRecord> findById(Long id) {
        return jdbcClient.sql("""
                SELECT id, url, last_updated, last_checked_at
                FROM links
                WHERE id = :id
                """).param("id", id).query(LINK_ROW_MAPPER).optional();
    }

    @Override
    public List<LinkRecord> findPage(int limit, int offset) {
        return jdbcClient
                .sql("""
                SELECT id, url, last_updated, last_checked_at
                FROM links
                ORDER BY id
                LIMIT :limit OFFSET :offset
                """)
                .param("limit", limit)
                .param("offset", offset)
                .query(LINK_ROW_MAPPER)
                .list();
    }

    @Override
    public List<LinkRecord> findLinksDueForUpdateCheck(Instant checkedBefore, int limit) {
        return jdbcClient
                .sql("""
                SELECT id, url, last_updated, last_checked_at
                FROM links
                WHERE last_checked_at < :checkedBefore
                ORDER BY last_checked_at ASC, id ASC
                LIMIT :limit
                """)
                .param("checkedBefore", Timestamp.from(checkedBefore))
                .param("limit", limit)
                .query(LINK_ROW_MAPPER)
                .list();
    }

    @Override
    public void updateLastUpdated(Long id, Instant lastUpdated) {
        jdbcClient
                .sql("""
                UPDATE links
                SET last_updated = :lastUpdated
                WHERE id = :id
                """)
                .param("id", id)
                .param("lastUpdated", toTimestamp(lastUpdated))
                .update();
    }

    @Override
    public void updateLastCheckedAt(Long id, Instant lastCheckedAt) {
        jdbcClient
                .sql("""
                UPDATE links
                SET last_checked_at = :lastCheckedAt
                WHERE id = :id
                """)
                .param("id", id)
                .param("lastCheckedAt", Timestamp.from(lastCheckedAt))
                .update();
    }

    @Override
    public void deleteById(Long id) {
        jdbcClient.sql("""
                DELETE FROM links
                WHERE id = :id
                """).param("id", id).update();
    }

    @Override
    public long countByUrlContaining(String domain) {
        return jdbcClient
                .sql("select count(*) from links where url like :pattern")
                .param("pattern", "%" + domain + "%")
                .query(Long.class)
                .single();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
