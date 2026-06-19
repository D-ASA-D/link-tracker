package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    SubscriptionRecord save(Long chatId, Long linkId);

    Optional<SubscriptionRecord> findByChatIdAndLinkId(Long chatId, Long linkId);

    List<SubscriptionRecord> findByChatId(Long chatId, int limit, int offset);

    List<SubscriptionRecord> findByLinkId(Long linkId);

    List<SubscriptionRecord> findByChatIdAndTag(Long chatId, String tagName);

    boolean existsByChatIdAndLinkId(Long chatId, Long linkId);

    void deleteById(Long id);

    long countByLinkId(Long linkId);
}
