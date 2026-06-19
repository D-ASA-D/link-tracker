package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.util.PreviewUtils;
import java.time.Instant;
import java.util.Comparator;
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
public class StackoverflowClient {

    private static final String STACKOVERFLOW_DOMAIN = "stackoverflow.com";
    private static final String EXTERNAL_SOURCE_SCOPE = "external_source";

    private static final String ITEMS_KEY = "items";
    private static final String TITLE_KEY = "title";
    private static final String BODY_KEY = "body";
    private static final String OWNER_KEY = "owner";
    private static final String DISPLAY_NAME_KEY = "display_name";
    private static final String ANSWER_TYPE = "ANSWER";
    private static final String COMMENT_TYPE = "COMMENT";

    private final RestTemplate restTemplate;
    private final StackoverflowProperties properties;
    private final ResilientHttpExecutor resilientHttpExecutor;
    private final ScrapperMetricsService metricsService;

    public Optional<LinkUpdateInfo> getLastUpdate(String url) {
        return metricsService.recordRequestDuration(
                EXTERNAL_SOURCE_SCOPE, STACKOVERFLOW_DOMAIN, () -> getLastUpdateInternal(url));
    }

    private Optional<LinkUpdateInfo> getLastUpdateInternal(String url) {
        String questionId = extractQuestionId(url);

        try {
            String questionTitle = fetchQuestionTitle(questionId);
            if (questionTitle == null) {
                return Optional.empty();
            }

            List<Map<String, Object>> answers = fetchAnswers(questionId);
            List<Map<String, Object>> comments = fetchComments(questionId);

            return selectLatestUpdate(questionTitle, answers, comments);
        } catch (Exception e) {
            log.error("stackoverflow_api_error url={}", url, e);
            throw new IllegalStateException("StackOverflow API unavailable for url: " + url, e);
        }
    }

    String extractQuestionId(String url) {
        String normalized = url == null ? "" : url.trim();

        if (!normalized.contains("/questions/")) {
            throw new IllegalArgumentException("Invalid StackOverflow URL: " + url);
        }

        String[] parts = normalized.split("/questions/", 2);
        String[] tail = parts[1].split("/");

        if (tail.length == 0 || tail[0].isBlank()) {
            throw new IllegalArgumentException("Invalid StackOverflow URL: " + url);
        }

        return tail[0];
    }

    String buildQuestionApiUrl(String questionId) {
        return withKey(properties.getBaseUrl() + "/questions/" + questionId + "?site=stackoverflow&filter=withbody");
    }

    String buildAnswersApiUrl(String questionId) {
        return withKey(
                properties.getBaseUrl() + "/questions/" + questionId + "/answers?site=stackoverflow&filter=withbody");
    }

    String buildCommentsApiUrl(String questionId) {
        return withKey(
                properties.getBaseUrl() + "/questions/" + questionId + "/comments?site=stackoverflow&filter=withbody");
    }

    private String withKey(String url) {
        if (properties.getKey() != null && !properties.getKey().isBlank()) {
            return url + "&key=" + properties.getKey();
        }
        return url;
    }

    private String fetchQuestionTitle(String questionId) {
        List<Map<String, Object>> items = fetchItems(buildQuestionApiUrl(questionId));
        if (items.isEmpty()) {
            return null;
        }

        return (String) items.get(0).get(TITLE_KEY);
    }

    private List<Map<String, Object>> fetchAnswers(String questionId) {
        return fetchItems(buildAnswersApiUrl(questionId));
    }

    private List<Map<String, Object>> fetchComments(String questionId) {
        return fetchItems(buildCommentsApiUrl(questionId));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchItems(String apiUrl) {
        Map<String, Object> response =
                resilientHttpExecutor.execute(() -> restTemplate.getForObject(apiUrl, Map.class));

        if (response == null || !response.containsKey(ITEMS_KEY)) {
            return List.of();
        }

        return (List<Map<String, Object>>) response.get(ITEMS_KEY);
    }

    private Optional<LinkUpdateInfo> selectLatestUpdate(
            String questionTitle, List<Map<String, Object>> answers, List<Map<String, Object>> comments) {

        Optional<LinkUpdateInfo> latestAnswer = findLatestAnswer(questionTitle, answers);
        Optional<LinkUpdateInfo> latestComment = findLatestComment(questionTitle, comments);

        if (latestAnswer.isEmpty() && latestComment.isEmpty()) {
            return Optional.empty();
        }

        if (latestAnswer.isPresent() && latestComment.isPresent()) {
            return pickLatest(latestAnswer.orElseThrow(), latestComment.orElseThrow());
        }

        return latestAnswer.isPresent() ? latestAnswer : latestComment;
    }

    private Optional<LinkUpdateInfo> findLatestAnswer(String questionTitle, List<Map<String, Object>> answers) {
        return answers.stream()
                .max(Comparator.comparing(this::extractCreationInstant))
                .map(answer -> toUpdateInfo(questionTitle, answer, ANSWER_TYPE));
    }

    private Optional<LinkUpdateInfo> findLatestComment(String questionTitle, List<Map<String, Object>> comments) {
        return comments.stream()
                .max(Comparator.comparing(this::extractCreationInstant))
                .map(comment -> toUpdateInfo(questionTitle, comment, COMMENT_TYPE));
    }

    private Optional<LinkUpdateInfo> pickLatest(LinkUpdateInfo answer, LinkUpdateInfo comment) {
        return answer.eventTime().isAfter(comment.eventTime()) ? Optional.of(answer) : Optional.of(comment);
    }

    @SuppressWarnings("unchecked")
    private LinkUpdateInfo toUpdateInfo(String questionTitle, Map<String, Object> item, String eventType) {
        Map<String, Object> owner = (Map<String, Object>) item.get(OWNER_KEY);
        String username = owner == null ? null : (String) owner.get(DISPLAY_NAME_KEY);
        Instant createdAt = extractCreationInstant(item);
        String body = (String) item.get(BODY_KEY);

        return new LinkUpdateInfo(
                createdAt, questionTitle, username, createdAt, PreviewUtils.plainTextPreview(body), eventType);
    }

    private Instant extractCreationInstant(Map<String, Object> item) {
        Number creationDate = (Number) item.get("creation_date");
        return creationDate == null ? Instant.EPOCH : Instant.ofEpochSecond(creationDate.longValue());
    }
}
