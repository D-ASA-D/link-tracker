package backend.academy.linktracker.scrapper.integration.orm;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.access-type=ORM")
class OrmSubscriptionRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private Long chatId;
    private Long linkId;

    @BeforeEach
    void init() {
        chatId = 401L;
        chatRepository.register(chatId);

        LinkRecord link = linkRepository.save("https://github.com/test/orm-subscription", Instant.now(), Instant.now());
        linkId = link.id();
    }

    @Test
    void saveAndExists_shouldWork() {
        SubscriptionRecord subscription = subscriptionRepository.save(chatId, linkId);

        assertThat(subscription.id()).isNotNull();
        assertThat(subscriptionRepository.existsByChatIdAndLinkId(chatId, linkId))
                .isTrue();
        assertThat(subscriptionRepository.findByChatIdAndLinkId(chatId, linkId)).isPresent();
    }

    @Test
    void deleteById_shouldWork() {
        SubscriptionRecord subscription = subscriptionRepository.save(chatId, linkId);

        subscriptionRepository.deleteById(subscription.id());

        assertThat(subscriptionRepository.existsByChatIdAndLinkId(chatId, linkId))
                .isFalse();
        assertThat(subscriptionRepository.findByChatIdAndLinkId(chatId, linkId)).isEmpty();
    }

    @Test
    void countByLinkId_shouldWork() {
        subscriptionRepository.save(chatId, linkId);

        assertThat(subscriptionRepository.countByLinkId(linkId)).isEqualTo(1);
    }

    @Test
    void findByLinkId_shouldReturnSubscription() {
        SubscriptionRecord subscription = subscriptionRepository.save(chatId, linkId);

        List<SubscriptionRecord> subscriptions = subscriptionRepository.findByLinkId(linkId);

        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.getFirst().id()).isEqualTo(subscription.id());
        assertThat(subscriptions.getFirst().chatId()).isEqualTo(chatId);
        assertThat(subscriptions.getFirst().linkId()).isEqualTo(linkId);
    }

    @Test
    void findByChatId_shouldReturnSubscription() {
        SubscriptionRecord subscription = subscriptionRepository.save(chatId, linkId);

        List<SubscriptionRecord> subscriptions = subscriptionRepository.findByChatId(chatId, 10, 0);

        assertThat(subscriptions).hasSize(1);
        assertThat(subscriptions.getFirst().id()).isEqualTo(subscription.id());
        assertThat(subscriptions.getFirst().chatId()).isEqualTo(chatId);
        assertThat(subscriptions.getFirst().linkId()).isEqualTo(linkId);
    }
}
