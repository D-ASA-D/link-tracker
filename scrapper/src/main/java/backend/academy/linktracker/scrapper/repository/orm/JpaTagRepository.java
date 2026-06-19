package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.orm.TagEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTagRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findByName(String name);

    List<TagEntity> findAllByOrderByIdAsc(Pageable pageable);

    @Query("""
        select t
        from TagEntity t
        join SubscriptionTagEntity st on st.tagId = t.id
        where st.subscriptionId = :subscriptionId
        order by t.id
    """)
    List<TagEntity> findAllBySubscriptionId(Long subscriptionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE tags
        SET name = :name
        WHERE id = :id
        """, nativeQuery = true)
    int updateNameById(@Param("id") Long id, @Param("name") String name);
}
