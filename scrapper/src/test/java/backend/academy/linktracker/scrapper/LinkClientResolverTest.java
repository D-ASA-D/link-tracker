package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.client.LinkClientResolver;
import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkClientResolverTest {

    private GithubClient github;
    private StackoverflowClient so;
    private LinkClientResolver resolver;

    @BeforeEach
    void setup() {
        github = mock(GithubClient.class);
        so = mock(StackoverflowClient.class);
        resolver = new LinkClientResolver(github, so);
    }

    @Test
    void resolve_callsGithub() {
        LinkUpdateInfo info = new LinkUpdateInfo(
                Instant.parse("2026-03-08T10:00:00Z"),
                "Issue title",
                "octocat",
                Instant.parse("2026-03-08T10:00:00Z"),
                "Preview text",
                "ISSUE");

        when(github.getLastUpdate("https://github.com/user/repo")).thenReturn(Optional.of(info));

        Optional<LinkUpdateInfo> result = resolver.resolve("https://github.com/user/repo");

        verify(github).getLastUpdate("https://github.com/user/repo");
        assertTrue(result.isPresent());
        assertEquals("Issue title", result.orElseThrow().title());
    }

    @Test
    void resolve_callsStackoverflow() {
        LinkUpdateInfo info = new LinkUpdateInfo(
                Instant.parse("2026-03-08T10:00:00Z"),
                "Question title",
                "Jon Skeet",
                Instant.parse("2026-03-08T10:00:00Z"),
                "Preview text",
                "ANSWER");

        when(so.getLastUpdate("https://stackoverflow.com/questions/123")).thenReturn(Optional.of(info));

        Optional<LinkUpdateInfo> result = resolver.resolve("https://stackoverflow.com/questions/123");

        verify(so).getLastUpdate("https://stackoverflow.com/questions/123");
        assertTrue(result.isPresent());
        assertEquals("Question title", result.orElseThrow().title());
    }

    @Test
    void resolve_throwsUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("https://example.com"));
    }
}
