package backend.academy.linktracker.scrapper.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyCacheType;
import backend.academy.linktracker.scrapper.properties.ValkeyProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
class LettuceClientSideLinksListCacheTest {

    private static final Long CHAT_ID = 805052108L;
    private static final int LIMIT = 100;
    private static final int OFFSET = 0;

    @Container
    private static final GenericContainer<?> VALKEY = new GenericContainer<>(DockerImageName.parse("valkey/valkey:8.1"))
            .withExposedPorts(6379)
            .withCommand("valkey-server", "--protected-mode", "no", "--save", "", "--appendonly", "no");

    @Test
    void shouldPutAndGetResponseFromValkeyThroughClientSideCache() {
        try (CacheTestContext context = createCache()) {
            ListLinksResponse response = responseWithOneLink();

            context.cache().put(CHAT_ID, LIMIT, OFFSET, response);

            ListLinksResponse result =
                    context.cache().get(CHAT_ID, LIMIT, OFFSET).orElseThrow();

            assertEquals(response, result);
        }
    }

    @Test
    void shouldEvictCachedResponse() {
        try (CacheTestContext context = createCache()) {
            ListLinksResponse response = responseWithOneLink();

            context.cache().put(CHAT_ID, LIMIT, OFFSET, response);
            context.cache().evict(CHAT_ID);

            assertTrue(context.cache().get(CHAT_ID, LIMIT, OFFSET).isEmpty());
        }
    }

    @Test
    void shouldExpireCachedResponseAfterTtl() throws InterruptedException {
        try (CacheTestContext context = createCache(Duration.ofSeconds(1))) {
            ListLinksResponse response = responseWithOneLink();

            context.cache().put(CHAT_ID, LIMIT, OFFSET, response);

            assertTrue(context.cache().get(CHAT_ID, LIMIT, OFFSET).isPresent());

            Thread.sleep(1500);

            assertTrue(context.cache().get(CHAT_ID, LIMIT, OFFSET).isEmpty());
        }
    }

    private CacheTestContext createCache() {
        return createCache(Duration.ofMinutes(10));
    }

    private CacheTestContext createCache(Duration ttl) {
        ValkeyProperties properties = new ValkeyProperties();
        properties.setCacheType(ValkeyCacheType.CLIENT_SIDE);
        properties.setCacheTtl(ttl);
        properties.setStandaloneNode(VALKEY.getHost() + ":" + VALKEY.getMappedPort(6379));
        properties.setPassword("");

        ObjectMapper objectMapper = JsonMapper.builder().build();

        RedisClient redisClient = RedisClient.create(createRedisUri(properties));
        redisClient.setOptions(ClientOptions.builder().autoReconnect(true).build());

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        Map<String, String> localCache = new ConcurrentHashMap<>();

        CacheFrontend<String, String> cacheFrontend =
                ClientSideCaching.enable(CacheAccessor.forMap(localCache), connection, TrackingArgs.Builder.enabled());

        LettuceClientSideLinksListCache cache =
                new LettuceClientSideLinksListCache(objectMapper, properties, connection, cacheFrontend, localCache);

        return new CacheTestContext(cache, connection, redisClient);
    }

    private RedisURI createRedisUri(ValkeyProperties properties) {
        String node = properties.getStandaloneNode();
        String[] parts = node.split(":");

        RedisURI.Builder builder = RedisURI.builder()
                .withHost(parts[0])
                .withPort(Integer.parseInt(parts[1]))
                .withTimeout(Duration.ofSeconds(5));

        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            builder.withPassword(properties.getPassword().toCharArray());
        }

        return builder.build();
    }

    private ListLinksResponse responseWithOneLink() {
        return new ListLinksResponse(
                List.of(new LinkResponse(
                        1L, "https://github.com/test/userrepo", List.of("java"), List.of("user=user"))),
                1);
    }

    private record CacheTestContext(
            LettuceClientSideLinksListCache cache,
            StatefulRedisConnection<String, String> connection,
            RedisClient redisClient)
            implements AutoCloseable {

        @Override
        public void close() {
            try {
                connection.close();
            } finally {
                redisClient.shutdown();
            }
        }
    }
}
