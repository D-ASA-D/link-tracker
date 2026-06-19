package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "scrapper")
public class ScrapperProperties {

    private String baseUrl;
}
