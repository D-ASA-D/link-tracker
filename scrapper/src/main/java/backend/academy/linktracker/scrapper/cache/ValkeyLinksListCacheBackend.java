package backend.academy.linktracker.scrapper.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyProperties;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "VALKEY", matchIfMissing = true)
public class ValkeyLinksListCacheBackend {

    private static final String KEY_PREFIX = "links:list:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ValkeyProperties properties;

    public Optional<ListLinksResponse> get(Long chatId, int limit, int offset) {
        String cacheKey = key(chatId);

        try {
            String json = redisTemplate.opsForValue().get(cacheKey);

            if (json == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(json, ListLinksResponse.class));
        } catch (JacksonException exception) {
            log.warn("valkey_links_list_cache_invalid_json chatId={} key={}", chatId, cacheKey, exception);
            evict(chatId);
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("valkey_links_list_cache_get_failed chatId={} key={}", chatId, cacheKey, exception);
            return Optional.empty();
        }
    }

    public void put(Long chatId, int limit, int offset, ListLinksResponse response) {
        String cacheKey = key(chatId);

        try {
            String json = objectMapper.writeValueAsString(response);

            redisTemplate.opsForValue().set(cacheKey, json, properties.getCacheTtl());

            log.debug("valkey_links_list_cache_put chatId={} key={}", chatId, cacheKey);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize links list response", exception);
        } catch (RuntimeException exception) {
            log.warn("valkey_links_list_cache_put_failed chatId={} key={}", chatId, cacheKey, exception);
        }
    }

    public void evict(Long chatId) {
        String cacheKey = key(chatId);

        try {
            redisTemplate.delete(cacheKey);
            log.debug("valkey_links_list_cache_evict chatId={} key={}", chatId, cacheKey);
        } catch (RuntimeException exception) {
            log.warn("valkey_links_list_cache_evict_failed chatId={} key={}", chatId, cacheKey, exception);
        }
    }

    public String key(Long chatId) {
        return KEY_PREFIX + chatId;
    }
}
