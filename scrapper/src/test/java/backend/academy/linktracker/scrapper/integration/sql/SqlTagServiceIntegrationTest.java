package backend.academy.linktracker.scrapper.integration.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.service.TagService;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.access-type=SQL")
class SqlTagServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TagService tagService;

    @Test
    void createGetUpdateDelete_shouldWork() {
        TagRecord created = tagService.createTag(" java ");

        assertThat(created.name()).isEqualTo("java");

        TagRecord loaded = tagService.getTag(created.id());
        assertThat(loaded.name()).isEqualTo("java");

        TagRecord updated = tagService.updateTag(created.id(), " spring ");
        assertThat(updated.name()).isEqualTo("spring");

        tagService.deleteTag(created.id());

        assertThatThrownBy(() -> tagService.getTag(created.id())).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createBlank_shouldThrow() {
        assertThatThrownBy(() -> tagService.createTag("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
