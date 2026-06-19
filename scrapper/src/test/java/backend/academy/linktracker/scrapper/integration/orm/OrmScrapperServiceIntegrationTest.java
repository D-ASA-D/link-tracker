package backend.academy.linktracker.scrapper.integration.orm;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.ScrapperService;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.access-type=ORM",
            "app.github.base-url=http://localhost:${wiremock.server.port}",
            "app.stackoverflow.base-url=http://localhost:${wiremock.server.port}"
        })
@EnableWireMock
class OrmScrapperServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ScrapperService scrapperService;

    @Autowired
    private LinkRepository linkRepository;

    @Test
    void addLink_shouldCreateLinkSubscriptionAndTags() {
        String url = "https://github.com/test/sql-service";

        stubFor(get(urlEqualTo("/repos/test/sql-service")).willReturn(okJson("""
                {
                  "updated_at": "2025-01-01T00:00:00Z"
                }
            """)));

        scrapperService.registerChat(701L);

        LinkResponse response =
                scrapperService.addLink(701L, new AddLinkRequest(url, List.of("java", "spring", "java"), List.of()));

        assertThat(response.id()).isNotNull();
        assertThat(response.url()).isEqualTo(url);
        assertThat(response.tags()).containsExactlyInAnyOrder("java", "spring");
        assertThat(linkRepository.findByUrl(url)).isPresent();
    }

    @Test
    void addDuplicateLinkForSameChat_shouldThrow() {
        String url = "https://github.com/test/sql-duplicate";

        stubFor(get(urlEqualTo("/repos/test/sql-duplicate")).willReturn(okJson("""
                {
                  "updated_at": "2025-01-01T00:00:00Z"
                }
            """)));

        scrapperService.registerChat(702L);
        scrapperService.addLink(702L, new AddLinkRequest(url, List.of("tag"), List.of()));

        assertThatThrownBy(() -> scrapperService.addLink(702L, new AddLinkRequest(url, List.of("tag"), List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listLinks_shouldReturnTrackedLinks() {
        String url = "https://github.com/test/sql-list";

        stubFor(get(urlEqualTo("/repos/test/sql-list")).willReturn(okJson("""
                {
                  "updated_at": "2025-01-01T00:00:00Z"
                }
            """)));

        scrapperService.registerChat(703L);
        scrapperService.addLink(703L, new AddLinkRequest(url, List.of("tag1"), List.of()));

        ListLinksResponse response = scrapperService.listLinks(703L);

        assertThat(response.size()).isEqualTo(1);
        assertThat(response.links()).extracting(LinkResponse::url).contains(url);
    }

    @Test
    void removeLink_shouldDeleteTracking() {
        String url = "https://github.com/test/sql-remove";

        stubFor(get(urlEqualTo("/repos/test/sql-remove")).willReturn(okJson("""
                {
                  "updated_at": "2025-01-01T00:00:00Z"
                }
            """)));

        scrapperService.registerChat(704L);
        scrapperService.addLink(704L, new AddLinkRequest(url, List.of("tag1"), List.of()));

        LinkResponse removed = scrapperService.removeLink(704L, new RemoveLinkRequest(url));

        assertThat(removed.url()).isEqualTo(url);
        assertThat(scrapperService.listLinks(704L).size()).isZero();
    }

    @Test
    void addLinkForMissingChat_shouldThrow() {
        String url = "https://github.com/test/sql-missing-chat";

        stubFor(get(urlEqualTo("/repos/test/sql-missing-chat")).willReturn(okJson("""
                {
                  "updated_at": "2025-01-01T00:00:00Z"
                }
            """)));

        assertThatThrownBy(() -> scrapperService.addLink(999999L, new AddLinkRequest(url, List.of(), List.of())))
                .isInstanceOf(NoSuchElementException.class);
    }
}
