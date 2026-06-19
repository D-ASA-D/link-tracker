package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.LinkRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {

    LinkRecord save(String url, Instant lastUpdated, Instant lastCheckedAt);

    Optional<LinkRecord> findByUrl(String url);

    Optional<LinkRecord> findById(Long id);

    List<LinkRecord> findPage(int limit, int offset);

    List<LinkRecord> findLinksDueForUpdateCheck(Instant checkedBefore, int limit);

    void updateLastUpdated(Long id, Instant lastUpdated);

    void updateLastCheckedAt(Long id, Instant lastCheckedAt);

    void deleteById(Long id);

    long countByUrlContaining(String domain);
}
