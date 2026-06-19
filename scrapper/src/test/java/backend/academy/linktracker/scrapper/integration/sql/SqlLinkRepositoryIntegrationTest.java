package backend.academy.linktracker.scrapper.integration.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@SpringBootTest(properties = "app.access-type=SQL")
class SqlLinkRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private LinkRepository linkRepository;

    @Test
    void saveFindByIdFindByUrl_shouldWork() {
        Instant updatedAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant checkedAt = Instant.parse("2025-01-01T00:10:00Z");

        LinkRecord saved = linkRepository.save("https://github.com/test/sql-link", updatedAt, checkedAt);

        assertThat(saved.id()).isNotNull();
        assertThat(linkRepository.findById(saved.id())).isPresent();
        assertThat(linkRepository.findByUrl("https://github.com/test/sql-link")).isPresent();
    }

    @Test
    void saveDuplicate_shouldThrow() {
        linkRepository.save("https://github.com/test/sql-dup", Instant.now(), Instant.now());

        assertThatThrownBy(() -> linkRepository.save("https://github.com/test/sql-dup", Instant.now(), Instant.now()))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Link already exists");
    }

    @Test
    void findPage_shouldReturnOrderedPage() {
        linkRepository.save("https://github.com/test/sql-page-1", Instant.now(), Instant.now());
        linkRepository.save("https://github.com/test/sql-page-2", Instant.now(), Instant.now());

        List<LinkRecord> page = linkRepository.findPage(10, 0);

        assertThat(page).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deleteById_shouldRemoveLink() {
        LinkRecord saved = linkRepository.save("https://github.com/test/sql-delete", Instant.now(), Instant.now());

        linkRepository.deleteById(saved.id());

        assertThat(linkRepository.findById(saved.id())).isEmpty();
    }

    @Test
    void findLinksDueForUpdateCheck_shouldReturnOldEntriesOnly() {
        Instant oldChecked = Instant.parse("2025-01-01T00:00:00Z");
        Instant newChecked = Instant.parse("2026-01-01T00:00:00Z");

        linkRepository.save("https://github.com/test/sql-due-1", Instant.now(), oldChecked);
        linkRepository.save("https://github.com/test/sql-due-2", Instant.now(), newChecked);

        List<LinkRecord> due = linkRepository.findLinksDueForUpdateCheck(Instant.parse("2025-06-01T00:00:00Z"), 10);

        assertThat(due)
                .extracting(LinkRecord::url)
                .contains("https://github.com/test/sql-due-1")
                .doesNotContain("https://github.com/test/sql-due-2");
    }
}
