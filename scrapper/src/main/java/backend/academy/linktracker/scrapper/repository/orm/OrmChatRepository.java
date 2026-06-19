package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {

    private final JpaChatRepository repository;

    @Transactional
    @Override
    public void register(Long chatId) {
        int inserted = repository.insertIfNotExists(chatId);
        if (inserted == 0) {
            throw new IllegalStateException("Chat already exists");
        }
    }

    @Transactional
    @Override
    public void unregister(Long chatId) {
        int deleted = repository.deleteIfExists(chatId);
        if (deleted == 0) {
            throw new NoSuchElementException("Chat not found");
        }
    }

    @Override
    public boolean exists(Long chatId) {
        return repository.existsById(chatId);
    }
}
