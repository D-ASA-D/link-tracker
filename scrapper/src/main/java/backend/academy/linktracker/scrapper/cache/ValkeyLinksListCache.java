package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "VALKEY", matchIfMissing = true)
public class ValkeyLinksListCache implements LinksListCache {

    private final ValkeyLinksListCacheBackend backend;

    @Override
    public Optional<ListLinksResponse> get(Long chatId, int limit, int offset) {
        return backend.get(chatId, limit, offset);
    }

    @Override
    public void put(Long chatId, int limit, int offset, ListLinksResponse response) {
        backend.put(chatId, limit, offset, response);
    }

    @Override
    public void evict(Long chatId) {
        backend.evict(chatId);
    }
}
