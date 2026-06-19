package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.properties.YandexGPTProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfiguration {

    @Bean
    public RestClient restClient(YandexGPTProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(
                (int) properties.getTimeout().getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getTimeout().getReadTimeout().toMillis());
        return RestClient.builder().requestFactory(factory).build();
    }
}
