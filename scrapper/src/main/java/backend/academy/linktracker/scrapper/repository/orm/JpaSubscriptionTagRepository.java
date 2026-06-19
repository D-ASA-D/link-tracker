package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.orm.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.model.orm.SubscriptionTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSubscriptionTagRepository extends JpaRepository<SubscriptionTagEntity, SubscriptionTagId> {

    long countByTagId(Long tagId);

    @Modifying
    @Query(value = """
        INSERT INTO subscription_tags (subscription_id, tag_id)
        VALUES (:subscriptionId, :tagId)
        ON CONFLICT (subscription_id, tag_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfNotExists(@Param("subscriptionId") Long subscriptionId, @Param("tagId") Long tagId);

    @Modifying
    @Query(value = """
        DELETE FROM subscription_tags
        WHERE subscription_id = :subscriptionId
        """, nativeQuery = true)
    int deleteBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
