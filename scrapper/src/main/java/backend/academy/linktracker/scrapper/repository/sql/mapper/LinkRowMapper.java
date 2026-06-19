package backend.academy.linktracker.scrapper.repository.sql.mapper;

import backend.academy.linktracker.scrapper.model.LinkRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;

public final class LinkRowMapper implements RowMapper<LinkRecord> {

    public static final LinkRowMapper INSTANCE = new LinkRowMapper();

    private LinkRowMapper() {}

    @Override
    public LinkRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastUpdated = rs.getTimestamp("last_updated");
        Timestamp lastCheckedAt = rs.getTimestamp("last_checked_at");

        return new LinkRecord(
                rs.getLong("id"),
                rs.getString("url"),
                lastUpdated == null ? null : lastUpdated.toInstant(),
                lastCheckedAt == null ? null : lastCheckedAt.toInstant());
    }
}
