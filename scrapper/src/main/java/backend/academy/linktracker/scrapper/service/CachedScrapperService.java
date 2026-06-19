package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.cache.LinksListCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.DeleteLinksByTagResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CachedScrapperService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_OFFSET = 0;

    private final ScrapperService scrapperService;
    private final LinksListCache linksListCache;

    public void registerChat(Long chatId) {
        scrapperService.registerChat(chatId);
    }

    public void unregisterChat(Long chatId) {
        scrapperService.unregisterChat(chatId);
        linksListCache.evict(chatId);
    }

    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        LinkResponse response = scrapperService.addLink(chatId, request);
        linksListCache.evict(chatId);

        return response;
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        LinkResponse response = scrapperService.removeLink(chatId, request);
        linksListCache.evict(chatId);

        return response;
    }

    public DeleteLinksByTagResponse removeLinksByTag(Long chatId, String tag) {
        DeleteLinksByTagResponse response = scrapperService.removeLinksByTag(chatId, tag);
        linksListCache.evict(chatId);

        return response;
    }

    public ListLinksResponse listLinks(Long chatId) {
        return listLinks(chatId, DEFAULT_LIMIT, DEFAULT_OFFSET);
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
    public ListLinksResponse listLinks(Long chatId, int limit, int offset) {
        Optional<ListLinksResponse> cachedResponse = linksListCache.get(chatId, limit, offset);
        log.info(
                "links_list_cache_lookup chatId={} limit={} offset={} result={}",
                chatId,
                limit,
                offset,
                cachedResponse.isEmpty() ? "miss" : "hit");
        return cachedResponse.orElseGet(() -> {
            ListLinksResponse response = scrapperService.listLinks(chatId, limit, offset);
            linksListCache.put(chatId, limit, offset, response);
            return response;
        });
    }
}
