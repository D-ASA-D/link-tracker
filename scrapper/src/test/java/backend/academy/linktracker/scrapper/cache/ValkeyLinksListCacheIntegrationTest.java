package backend.academy.linktracker.scrapper.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.configuration.ValkeyConfiguration;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class ValkeyLinksListCacheIntegrationTest {

    @Container
    private static final GenericContainer<?> VALKEY = new GenericContainer<>("valkey/valkey:8.1")
            .withExposedPorts(6379)
            .withCommand("valkey-server", "--protected-mode", "no", "--save", "", "--appendonly", "no");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "app.valkey.cache-type=VALKEY",
                    "app.valkey.cluster-nodes=" + VALKEY.getHost() + ":" + VALKEY.getMappedPort(6379),
                    "app.valkey.password=",
                    "app.valkey.cache-ttl=10s");

    @Test
    void shouldPutGetAndEvictLinksListResponse() {
        contextRunner.run(context -> {
            LinksListCache cache = context.getBean(LinksListCache.class);
            assertInstanceOf(ValkeyLinksListCache.class, cache);

            StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

            Long chatId = 805052108L;
            int limit = 100;
            int offset = 0;
            String cacheKey = "links:list:" + chatId;

            ListLinksResponse response = objectMapper.readValue("""
                    {
                      "links": [
                        {
                          "id": 1,
                          "url": "https://github.com/D-ASA-D/RSREU",
                          "tags": ["java"],
                          "filters": []
                        }
                      ],
                      "size": 1
                    }
                    """, ListLinksResponse.class);

            cache.put(chatId, limit, offset, response);

            String rawJson = redisTemplate.opsForValue().get(cacheKey);
            assertNotNull(rawJson);
            assertTrue(rawJson.contains("https://github.com/D-ASA-D/RSREU"));

            Long ttl = redisTemplate.getExpire(cacheKey);
            assertNotNull(ttl);
            assertTrue(ttl > 0);
            assertTrue(ttl <= Duration.ofSeconds(10).toSeconds());

            assertEquals(response, cache.get(chatId, limit, offset).orElseThrow());

            cache.evict(chatId);

            assertNull(redisTemplate.opsForValue().get(cacheKey));
            assertTrue(cache.get(chatId, limit, offset).isEmpty());
        });
    }

    @Test
    void shouldExpireCacheEntryAfterTtl() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestConfig.class)
                .withPropertyValues(
                        "app.valkey.cache-type=VALKEY",
                        "app.valkey.cluster-nodes=" + VALKEY.getHost() + ":" + VALKEY.getMappedPort(6379),
                        "app.valkey.password=",
                        "app.valkey.cache-ttl=1s")
                .run(context -> {
                    LinksListCache cache = context.getBean(LinksListCache.class);
                    assertInstanceOf(ValkeyLinksListCache.class, cache);

                    Long chatId = 805052108L;
                    int limit = 100;
                    int offset = 0;
                    String cacheKey = "links:list:" + chatId;

                    ListLinksResponse response = new ListLinksResponse(List.of(), 0);

                    cache.put(chatId, limit, offset, response);

                    assertTrue(cache.get(chatId, limit, offset).isPresent());
                    assertNotNull(context.getBean(StringRedisTemplate.class)
                            .opsForValue()
                            .get(cacheKey));

                    Thread.sleep(1500);

                    assertTrue(cache.get(chatId, limit, offset).isEmpty());
                    assertNull(context.getBean(StringRedisTemplate.class)
                            .opsForValue()
                            .get(cacheKey));
                });
    }

    @TestConfiguration
    @Import({ValkeyConfiguration.class, ValkeyLinksListCacheBackend.class, ValkeyLinksListCache.class})
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
