package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyProperties;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheFrontend;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "CLIENT_SIDE")
public class LettuceClientSideLinksListCache implements LinksListCache {

    private static final String KEY_PREFIX = "links:list:";

    private final ObjectMapper objectMapper;
    private final ValkeyProperties properties;
    private final StatefulRedisConnection<String, String> connection;
    private final CacheFrontend<String, String> cacheFrontend;
    private final Map<String, String> clientSideLocalCache;

    @Override
    public Optional<ListLinksResponse> get(Long chatId, int limit, int offset) {
        String cacheKey = key(chatId, limit, offset);

        try {
            String json = cacheFrontend.get(cacheKey);

            if (json == null) {
                return Optional.empty();
            }

            log.info("links_list_native_client_side_cache_hit chatId={} limit={} offset={}", chatId, limit, offset);

            return Optional.of(objectMapper.readValue(json, ListLinksResponse.class));
        } catch (JacksonException exception) {
            log.error("links_list_native_client_side_cache_invalid_json chatId={} key={}", chatId, cacheKey, exception);
            evict(chatId, limit, offset);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.error("links_list_native_client_side_cache_get_failed chatId={} key={}", chatId, cacheKey, exception);
            return Optional.empty();
        }
    }

    @Override
    public void put(Long chatId, int limit, int offset, ListLinksResponse response) {
        String cacheKey = key(chatId, limit, offset);

        try {
            String json = objectMapper.writeValueAsString(response);

            connection.sync().setex(cacheKey, properties.getCacheTtl().toSeconds(), json);
            clientSideLocalCache.remove(cacheKey);

            log.info("links_list_native_client_side_cache_put chatId={} limit={} offset={}", chatId, limit, offset);
        } catch (JacksonException exception) {
            log.error(
                    "links_list_native_client_side_cache_serialization_failed chatId={} key={}",
                    chatId,
                    cacheKey,
                    exception);
        } catch (RuntimeException exception) {
            log.error("links_list_native_client_side_cache_put_failed chatId={} key={}", chatId, cacheKey, exception);
        }
    }

    @Override
    public void evict(Long chatId) {
        String cacheKeyPrefix = KEY_PREFIX + chatId + ":";

        try {
            connection.sync().keys(cacheKeyPrefix + "*").forEach(key -> connection
                    .sync()
                    .del(key));

            clientSideLocalCache.keySet().removeIf(key -> key.startsWith(cacheKeyPrefix));

            log.info("links_list_native_client_side_cache_evict chatId={}", chatId);
        } catch (RuntimeException exception) {
            log.error("links_list_native_client_side_cache_evict_failed chatId={}", chatId, exception);
        }
    }

    private void evict(Long chatId, int limit, int offset) {
        String cacheKey = key(chatId, limit, offset);

        try {
            connection.sync().del(cacheKey);
            clientSideLocalCache.remove(cacheKey);

            log.info(
                    "links_list_native_client_side_cache_evict_key chatId={} limit={} offset={}",
                    chatId,
                    limit,
                    offset);
        } catch (RuntimeException exception) {
            log.error(
                    "links_list_native_client_side_cache_evict_key_failed chatId={} key={}",
                    chatId,
                    cacheKey,
                    exception);
        }
    }

    private String key(Long chatId, int limit, int offset) {
        return KEY_PREFIX + chatId + ":limit:" + limit + ":offset:" + offset;
    }
}
