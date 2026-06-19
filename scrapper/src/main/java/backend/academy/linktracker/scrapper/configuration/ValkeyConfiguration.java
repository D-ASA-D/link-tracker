package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ValkeyProperties;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(ValkeyProperties.class)
public class ValkeyConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "VALKEY", matchIfMissing = true)
    public LettuceConnectionFactory valkeyConnectionFactory(ValkeyProperties properties) {
        RedisConfiguration configuration = createConfiguration(properties);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey", name = "cache-type", havingValue = "VALKEY", matchIfMissing = true)
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    private RedisConfiguration createConfiguration(ValkeyProperties properties) {
        List<String> nodes = properties.getClusterNodes();

        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalStateException("app.valkey.cluster-nodes must not be empty when cache-type=VALKEY");
        }

        if (nodes.size() == 1) {
            RedisStandaloneConfiguration configuration = createStandaloneConfiguration(nodes.getFirst());
            setPassword(configuration, properties);
            return configuration;
        }

        RedisClusterConfiguration configuration = new RedisClusterConfiguration(nodes);
        setPassword(configuration, properties);
        return configuration;
    }

    private RedisStandaloneConfiguration createStandaloneConfiguration(String node) {
        String[] parts = node.split(":");

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(parts[0]);
        configuration.setPort(Integer.parseInt(parts[1]));

        return configuration;
    }

    private void setPassword(RedisConfiguration.WithPassword configuration, ValkeyProperties properties) {
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            configuration.setPassword(RedisPassword.of(properties.getPassword()));
        }
    }
}
