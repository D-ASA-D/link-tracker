package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.util.PreviewUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubClient {

    private static final String GITHUB_DOMAIN = "github.com";
    private static final String EXTERNAL_SOURCE_SCOPE = "external_source";

    private final RestTemplate restTemplate;
    private final GithubProperties properties;
    private final ResilientHttpExecutor resilientHttpExecutor;
    private final ScrapperMetricsService metricsService;

    public Optional<LinkUpdateInfo> getLastUpdate(String url) {
        return metricsService.recordRequestDuration(
                EXTERNAL_SOURCE_SCOPE, GITHUB_DOMAIN, () -> getLastUpdateInternal(url));
    }

    @SuppressWarnings("unchecked")
    private Optional<LinkUpdateInfo> getLastUpdateInternal(String url) {
        String apiUrl = convertToIssuesApiUrl(url);

        log.debug("github_api_request url={} apiUrl={}", url, apiUrl);

        try {
            List<Map<String, Object>> items =
                    resilientHttpExecutor.execute(() -> restTemplate.getForObject(apiUrl, List.class));

            if (items == null || items.isEmpty()) {
                log.warn("github_api_empty_response url={}", url);
                return Optional.empty();
            }

            Map<String, Object> item = items.get(0);

            String title = (String) item.get("title");
            String body = (String) item.get("body");
            String createdAtRaw = (String) item.get("created_at");

            Map<String, Object> user = (Map<String, Object>) item.get("user");
            String username = user == null ? null : (String) user.get("login");

            String eventType = item.containsKey("pull_request") ? "PR" : "ISSUE";

            Instant eventTime = createdAtRaw == null ? null : Instant.parse(createdAtRaw);

            return Optional.of(new LinkUpdateInfo(
                    eventTime, title, username, eventTime, PreviewUtils.plainTextPreview(body), eventType));
        } catch (Exception e) {
            log.error("github_api_error url={}", url, e);
            throw new IllegalStateException("GitHub API unavailable for url: " + url, e);
        }
    }

    String convertToIssuesApiUrl(String url) {
        String repoPath = extractRepoPath(url);
        return properties.getBaseUrl() + "/repos/" + repoPath + "/issues?sort=created&direction=desc&per_page=1";
    }

    String extractRepoPath(String url) {
        String normalized = url.trim();

        if (!normalized.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Invalid GitHub URL: " + url);
        }

        String path = normalized.substring("https://github.com/".length());
        String[] parts = path.split("/");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + url);
        }

        return parts[0] + "/" + parts[1];
    }
}
