package backend.academy.linktracker.ai.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.yandex.gpt")
@Getter
@Setter
@Validated
public class YandexGPTProperties {

    private ApiProperties apiProperties = new ApiProperties();

    private FolderSettings folderSettings = new FolderSettings();

    private Prompt prompt = new Prompt();

    private Timeout timeout = new Timeout();

    private String model;

    private int maxTokens;

    private double temperature;

    @Getter
    @Setter
    public static class ApiProperties {

        private String key;

        private String baseUrl;
    }

    @Getter
    @Setter
    public static class FolderSettings {

        private String id;
    }

    @Getter
    @Setter
    public static class Prompt {

        private String system = """
                Summarize the following update preview in 2-3 sentences.
                Keep the main meaning.
                Answer in Russian.
                Return only the summarized preview text.
                Do not add title, link, metadata or explanations.
                """;
    }

    @Getter
    @Setter
    public static class Timeout {

        private Duration connectTimeout = Duration.ofSeconds(2);

        private Duration readTimeout = Duration.ofSeconds(10);
    }
}
