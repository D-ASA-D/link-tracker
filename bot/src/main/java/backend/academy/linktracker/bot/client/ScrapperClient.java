package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.DeleteLinksByTagResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import backend.academy.linktracker.bot.dto.RenameTagRequest;
import backend.academy.linktracker.bot.dto.RenameTagResponse;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.properties.ScrapperProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapperClient {

    private static final String SCRAPPER_SYNC_API_SCOPE = "scrapper_sync_api";

    private final RestTemplate restTemplate;
    private final ScrapperProperties properties;
    private final BotMetricsService metricsService;

    private String baseUrl() {
        return properties.getBaseUrl();
    }

    public void registerChat(Long chatId) {
        metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "registerChat", () -> {
            log.info("Registering chat in scrapper. chatId={}", chatId);

            restTemplate.postForEntity(baseUrl() + "/tg-chat/" + chatId, null, Void.class);

            log.debug("Chat registered successfully. chatId={}", chatId);
        });
    }

    public void deleteChat(Long chatId) {
        metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "deleteChat", () -> {
            log.info("Deleting chat in scrapper. chatId={}", chatId);

            restTemplate.exchange(baseUrl() + "/tg-chat/" + chatId, HttpMethod.DELETE, null, Void.class);

            log.debug("Chat deleted successfully. chatId={}", chatId);
        });
    }

    public void addLink(Long chatId, String url, List<String> tags) {
        metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "addLink", () -> {
            log.info("Adding link. chatId={}, url={}, tags={}", chatId, url, tags);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Tg-Chat-Id", chatId.toString());

            AddLinkRequest request = new AddLinkRequest(url, tags, List.of());

            HttpEntity<AddLinkRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(baseUrl() + "/links", HttpMethod.POST, entity, Void.class);

            log.debug("Link successfully added. chatId={}, url={}", chatId, url);
        });
    }

    public void removeLink(Long chatId, String url) {
        metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "removeLink", () -> {
            log.info("Removing link. chatId={}, url={}", chatId, url);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Tg-Chat-Id", chatId.toString());

            RemoveLinkRequest request = new RemoveLinkRequest(url);

            HttpEntity<RemoveLinkRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(baseUrl() + "/links", HttpMethod.DELETE, entity, Void.class);

            log.debug("Link removed successfully. chatId={}, url={}", chatId, url);
        });
    }

    public ListLinksResponse getLinks(Long chatId) {
        return metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "getLinks", () -> {
            log.debug("Requesting links list. chatId={}", chatId);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Tg-Chat-Id", chatId.toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ListLinksResponse> response =
                    restTemplate.exchange(baseUrl() + "/links", HttpMethod.GET, entity, ListLinksResponse.class);

            return response.getBody();
        });
    }

    public RenameTagResponse renameTag(Long chatId, RenameTagRequest request) {
        return metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "renameTag", () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-Chat-Id", String.valueOf(chatId));

            HttpEntity<RenameTagRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<RenameTagResponse> response =
                    restTemplate.exchange(baseUrl() + "/tags", HttpMethod.PUT, entity, RenameTagResponse.class);

            return response.getBody();
        });
    }

    public DeleteLinksByTagResponse removeLinksByTag(Long chatId, String tag) {
        return metricsService.recordCommandDuration(SCRAPPER_SYNC_API_SCOPE, "removeLinksByTag", () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tg-Chat-Id", String.valueOf(chatId));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<DeleteLinksByTagResponse> response = restTemplate.exchange(
                    baseUrl() + "/links/by-tag/" + tag, HttpMethod.DELETE, entity, DeleteLinksByTagResponse.class);

            return response.getBody();
        });
    }
}
