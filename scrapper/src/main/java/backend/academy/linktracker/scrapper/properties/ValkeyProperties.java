package backend.academy.linktracker.scrapper.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.valkey")
public class ValkeyProperties {

    private ValkeyCacheType cacheType = ValkeyCacheType.VALKEY;

    private List<String> clusterNodes = new ArrayList<>();

    private String standaloneNode;

    private String password;

    private Duration cacheTtl = Duration.ofMinutes(10);
}
