package backend.academy.linktracker.scrapper.properties;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.resilience")
public class ResilienceProperties {

    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Timeout {
        private Duration connectTimeout = Duration.ofSeconds(1);
        private Duration readTimeout = Duration.ofSeconds(2);
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private Duration waitDuration = Duration.ofMillis(500);
        private BackoffStrategy backoffStrategy = BackoffStrategy.CONSTANT;
        private double exponentialMultiplier = 2.0;
        private List<Integer> retryableStatuses = List.of(500, 502, 503, 504, 408, 429);
    }

    @Getter
    @Setter
    public static class CircuitBreaker {
        private int slidingWindowSize = 10;
        private int minimumNumberOfCalls = 5;
        private float failureRateThreshold = 50;
        private int permittedCallsInHalfOpenState = 3;
        private Duration waitDurationInOpenState = Duration.ofSeconds(5);
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int limitForPeriod = 60;
        private Duration refreshPeriod = Duration.ofMinutes(1);
        private Duration timeoutDuration = Duration.ZERO;
    }

    public enum BackoffStrategy {
        CONSTANT,
        EXPONENTIAL
    }
}
