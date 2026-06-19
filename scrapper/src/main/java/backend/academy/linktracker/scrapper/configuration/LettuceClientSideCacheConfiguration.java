package backend.academy.linktracker.scrapper.configuration;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "CLIENT_SIDE")
public class LettuceClientSideCacheConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedisClient clientSideRedisClient(ValkeyProperties properties) {
        RedisClient redisClient = RedisClient.create(createRedisUri(properties));
        redisClient.setOptions(ClientOptions.builder().autoReconnect(true).build());
        return redisClient;
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, String> clientSideRedisConnection(RedisClient clientSideRedisClient) {
        return clientSideRedisClient.connect();
    }

    @Bean
    public Map<String, String> clientSideLocalCache() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public CacheFrontend<String, String> clientSideCacheFrontend(
            StatefulRedisConnection<String, String> clientSideRedisConnection,
            Map<String, String> clientSideLocalCache) {
        return ClientSideCaching.enable(
                CacheAccessor.forMap(clientSideLocalCache), clientSideRedisConnection, TrackingArgs.Builder.enabled());
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
}
