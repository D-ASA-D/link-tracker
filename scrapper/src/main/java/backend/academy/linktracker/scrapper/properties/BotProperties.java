package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.bot")
@Getter
@Setter
@Validated
public class BotProperties {

    @NotEmpty
    private String baseUrl;
}
