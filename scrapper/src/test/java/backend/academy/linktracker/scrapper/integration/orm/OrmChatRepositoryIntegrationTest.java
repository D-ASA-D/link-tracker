package backend.academy.linktracker.scrapper.integration.orm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@SpringBootTest(properties = "app.access-type=ORM")
class OrmChatRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Test
    void registerAndExists_shouldWork() {
        chatRepository.register(201L);

        assertThat(chatRepository.exists(201L)).isTrue();
    }

    @Test
    void registerDuplicate_shouldThrow() {
        chatRepository.register(202L);

        assertThatThrownBy(() -> chatRepository.register(202L)).isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @Test
    void unregister_shouldRemoveChat() {
        chatRepository.register(203L);

        chatRepository.unregister(203L);

        assertThat(chatRepository.exists(203L)).isFalse();
    }

    @Test
    void unregisterMissing_shouldThrow() {
        assertThatThrownBy(() -> chatRepository.unregister(999L)).isInstanceOf(NoSuchElementException.class);
    }
}
