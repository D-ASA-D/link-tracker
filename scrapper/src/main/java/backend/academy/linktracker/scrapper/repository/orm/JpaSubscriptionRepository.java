package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.orm.SubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    Optional<SubscriptionEntity> findByChatIdAndLinkId(Long chatId, Long linkId);

    List<SubscriptionEntity> findByChatIdOrderByIdAsc(Long chatId, Pageable pageable);

    List<SubscriptionEntity> findByLinkIdOrderById(Long linkId);

    boolean existsByChatIdAndLinkId(Long chatId, Long linkId);

    long countByLinkId(Long linkId);

    @Query("""
        select s
        from SubscriptionEntity s
        join SubscriptionTagEntity st on st.subscriptionId = s.id
        join TagEntity t on t.id = st.tagId
        where s.chatId = :chatId
          and t.name = :tagName
        order by s.id asc
    """)
    List<SubscriptionEntity> findByChatIdAndTagOrderById(Long chatId, String tagName);
}
