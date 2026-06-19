package backend.academy.linktracker.scrapper.integration.sql;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.access-type=SQL")
class SqlTagRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private Long subscriptionId;

    @BeforeEach
    void init() {
        chatRepository.register(501L);
        LinkRecord link = linkRepository.save("https://github.com/test/sql-tag-link", Instant.now(), Instant.now());
        SubscriptionRecord subscription = subscriptionRepository.save(501L, link.id());
        subscriptionId = subscription.id();
    }

    @Test
    void findOrCreateAndFindById_shouldWork() {
        TagRecord tag = tagRepository.findOrCreate("java");

        assertThat(tag.id()).isNotNull();
        assertThat(tagRepository.findById(tag.id())).isPresent();
        assertThat(tagRepository.findByName("java")).isPresent();
    }

    @Test
    void update_shouldWork() {
        TagRecord tag = tagRepository.findOrCreate("backend");

        TagRecord updated = tagRepository.update(tag.id(), "spring");

        assertThat(updated.name()).isEqualTo("spring");
    }

    @Test
    void attachAndFindBySubscriptionId_shouldWork() {
        TagRecord tag = tagRepository.findOrCreate("postgres");

        tagRepository.attachToSubscription(subscriptionId, tag.id());

        assertThat(tagRepository.findBySubscriptionId(subscriptionId))
                .extracting(TagRecord::name)
                .contains("postgres");
    }

    @Test
    void detachAllFromSubscription_shouldWork() {
        TagRecord tag = tagRepository.findOrCreate("liquibase");
        tagRepository.attachToSubscription(subscriptionId, tag.id());

        tagRepository.detachAllFromSubscription(subscriptionId);

        assertThat(tagRepository.findBySubscriptionId(subscriptionId)).isEmpty();
    }

    @Test
    void deleteById_shouldWork() {
        TagRecord tag = tagRepository.findOrCreate("cleanup");

        tagRepository.deleteById(tag.id());

        assertThat(tagRepository.findById(tag.id())).isEmpty();
    }
}
