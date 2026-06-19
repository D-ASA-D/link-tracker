package backend.academy.linktracker.scrapper.repository.sql.mapper;

import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public final class SubscriptionRowMapper implements RowMapper<SubscriptionRecord> {

    public static final SubscriptionRowMapper INSTANCE = new SubscriptionRowMapper();

    private SubscriptionRowMapper() {}

    @Override
    public SubscriptionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SubscriptionRecord(rs.getLong("id"), rs.getLong("chat_id"), rs.getLong("link_id"));
    }
}
