package backend.academy.linktracker.scrapper.integration.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@SpringBootTest(properties = "app.access-type=SQL")
class SqlChatRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Test
    void registerAndExists_shouldWork() {
        chatRepository.register(101L);

        assertThat(chatRepository.exists(101L)).isTrue();
    }

    @Test
    void registerDuplicate_shouldThrow() {
        chatRepository.register(102L);

        assertThatThrownBy(() -> chatRepository.register(102L))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Chat already exists");
    }

    @Test
    void unregister_shouldRemoveChat() {
        chatRepository.register(103L);

        chatRepository.unregister(103L);

        assertThat(chatRepository.exists(103L)).isFalse();
    }

    @Test
    void unregisterMissing_shouldThrow() {
        assertThatThrownBy(() -> chatRepository.unregister(999L)).isInstanceOf(NoSuchElementException.class);
    }
}
