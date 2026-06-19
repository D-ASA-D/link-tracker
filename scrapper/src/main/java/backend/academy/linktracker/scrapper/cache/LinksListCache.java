package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.util.Optional;

public interface LinksListCache {

    Optional<ListLinksResponse> get(Long chatId, int limit, int offset);

    void put(Long chatId, int limit, int offset, ListLinksResponse response);

    void evict(Long chatId);
}
