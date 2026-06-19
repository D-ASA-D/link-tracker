package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.model.orm.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.access-type", havingValue = "ORM")
public class OrmSubscriptionRepository implements SubscriptionRepository {

    private final JpaSubscriptionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SubscriptionRecord save(Long chatId, Long linkId) {
        try {
            SubscriptionEntity entity = new SubscriptionEntity();
            entity.setChatId(chatId);
            entity.setLinkId(linkId);
            return map(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Subscription already exists");
        }
    }

    @Override
    public Optional<SubscriptionRecord> findByChatIdAndLinkId(Long chatId, Long linkId) {
        return repository.findByChatIdAndLinkId(chatId, linkId).map(this::map);
    }

    @Override
    public List<SubscriptionRecord> findByChatId(Long chatId, int limit, int offset) {
        return entityManager
                .createQuery("""
                select s
                from SubscriptionEntity s
                where s.chatId = :chatId
                order by s.id asc
                """, SubscriptionEntity.class)
                .setParameter("chatId", chatId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<SubscriptionRecord> findByLinkId(Long linkId) {
        return repository.findByLinkIdOrderById(linkId).stream().map(this::map).toList();
    }

    @Override
    public boolean existsByChatIdAndLinkId(Long chatId, Long linkId) {
        return repository.existsByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long countByLinkId(Long linkId) {
        return repository.countByLinkId(linkId);
    }

    @Override
    public List<SubscriptionRecord> findByChatIdAndTag(Long chatId, String tagName) {
        return repository.findByChatIdAndTagOrderById(chatId, tagName).stream()
                .map(this::map)
                .toList();
    }

    private SubscriptionRecord map(SubscriptionEntity entity) {
        return new SubscriptionRecord(entity.getId(), entity.getChatId(), entity.getLinkId());
    }
}
