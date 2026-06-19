package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ResilienceProperties.class)
public class ClientConfiguration {

    @Bean
    public RestTemplate restTemplate(ResilienceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(
                (int) properties.getTimeout().getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getTimeout().getReadTimeout().toMillis());
        return new RestTemplate(factory);
    }
}
