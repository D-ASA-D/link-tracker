package backend.academy.linktracker.ai.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai-agent")
public class AiAgentProperties {

    private Kafka kafka = new Kafka();

    private Filter filter = new Filter();

    private Summarization summarization = new Summarization();

    private Prioritization prioritization = new Prioritization();

    private Grouping grouping = new Grouping();

    @Getter
    @Setter
    public static class Kafka {
        private String rawUpdatesTopic = "raw-updates-topic";
        private String processedUpdatesTopic = "processed-updates-topic";
    }

    @Getter
    @Setter
    public static class Filter {
        private List<String> stopWords = new ArrayList<>();
        private List<String> excludedAuthors = new ArrayList<>();
        private int minLength = 20;
    }

    @Getter
    @Setter
    public static class Summarization {
        private long threshold = 500;
        private Mode mode = Mode.STUB;
    }

    @Getter
    @Setter
    public static class Prioritization {
        private List<String> highKeywords = new ArrayList<>();
        private List<String> lowKeywords = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Grouping {
        private long windowMs = 30000;
    }

    public enum Mode {
        STUB,
        YANDEX_GPT
    }
}
