package backend.academy.linktracker.scrapper.repository.sql.mapper;

import backend.academy.linktracker.scrapper.model.TagRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public final class TagRowMapper implements RowMapper<TagRecord> {

    public static final TagRowMapper INSTANCE = new TagRowMapper();

    private TagRowMapper() {}

    @Override
    public TagRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TagRecord(rs.getLong("id"), rs.getString("name"));
    }
}
