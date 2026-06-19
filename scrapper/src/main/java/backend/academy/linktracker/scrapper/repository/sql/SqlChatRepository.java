package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlChatRepository implements ChatRepository {

    private final JdbcClient jdbcClient;

    @Override
    public void register(Long chatId) {
        try {
            jdbcClient
                    .sql("INSERT INTO chats(id) VALUES (:id)")
                    .param("id", chatId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("Chat already exists");
        }
    }

    @Override
    public void unregister(Long chatId) {
        int updated = jdbcClient
                .sql("DELETE FROM chats WHERE id = :id")
                .param("id", chatId)
                .update();

        if (updated == 0) {
            throw new NoSuchElementException("Chat not found");
        }
    }

    @Override
    public boolean exists(Long chatId) {
        Long count = jdbcClient
                .sql("SELECT COUNT(*) FROM chats WHERE id = :id")
                .param("id", chatId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }
}
