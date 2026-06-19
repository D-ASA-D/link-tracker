package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "NONE")
public class NoOpLinksListCache implements LinksListCache {

    @Override
    public Optional<ListLinksResponse> get(Long chatId, int limit, int offset) {
        return Optional.empty();
    }

    @Override
    public void put(Long chatId, int limit, int offset, ListLinksResponse response) {}

    @Override
    public void evict(Long chatId) {}
}
