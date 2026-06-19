package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.orm.LinkEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {

    Optional<LinkEntity> findByUrl(String url);

    List<LinkEntity> findAllByOrderByIdAsc(Pageable pageable);

    List<LinkEntity> findByLastCheckedAtBeforeOrderByLastCheckedAtAscIdAsc(Instant checkedBefore, Pageable pageable);

    long countByUrlContaining(String domain);

    @Modifying
    @Query(value = """
        UPDATE links
        SET last_updated = :lastUpdated
        WHERE id = :id
        """, nativeQuery = true)
    int updateLastUpdatedById(@Param("id") Long id, @Param("lastUpdated") Instant lastUpdated);

    @Modifying
    @Query(value = """
        UPDATE links
        SET last_checked_at = :lastCheckedAt
        WHERE id = :id
        """, nativeQuery = true)
    int updateLastCheckedAtById(@Param("id") Long id, @Param("lastCheckedAt") Instant lastCheckedAt);
}
