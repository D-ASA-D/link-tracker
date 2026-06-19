package backend.academy.linktracker.scrapper.integration.orm;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.access-type=ORM")
class OrmRepositoryWiringIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void shouldWireOrmRepositories() {
        assertThat(chatRepository.getClass().getName()).contains(".repository.orm.");
        assertThat(linkRepository.getClass().getName()).contains(".repository.orm.");
        assertThat(subscriptionRepository.getClass().getName()).contains(".repository.orm.");
        assertThat(tagRepository.getClass().getName()).contains(".repository.orm.");
    }
}
