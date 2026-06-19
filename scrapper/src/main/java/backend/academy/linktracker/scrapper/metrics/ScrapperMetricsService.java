package backend.academy.linktracker.scrapper.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScrapperMetricsService {

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> apiRequestCounters = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> requestDurationMetrics = new ConcurrentHashMap<>();

    public void incrementApiRequest(String source) {
        apiRequestCounters
                .computeIfAbsent(source, this::createApiRequestCounter)
                .increment();
    }

    public <T> T recordRequestDuration(String scope, String scopeType, Supplier<T> action) {
        long startedAt = System.nanoTime();

        try {
            return action.get();
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            requestDurationMetric(scope, scopeType).record(durationMs);
        }
    }

    public void recordRequestDuration(String scope, String scopeType, Runnable action) {
        long startedAt = System.nanoTime();

        try {
            action.run();
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            requestDurationMetric(scope, scopeType).record(durationMs);
        }
    }

    private Counter createApiRequestCounter(String source) {
        return Counter.builder("api_requests")
                .description("Count of incoming Scrapper API requests")
                .tag("source", source)
                .register(meterRegistry);
    }

    private DistributionSummary requestDurationMetric(String scope, String scopeType) {
        String key = scope + ":" + scopeType;

        return requestDurationMetrics.computeIfAbsent(
                key, ignored -> DistributionSummary.builder("request_duration_ms_total")
                        .description("Duration of Scrapper operations in milliseconds")
                        .baseUnit("milliseconds")
                        .tag("scope", scope)
                        .tag("scope_type", scopeType)
                        .serviceLevelObjectives(10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000)
                        .publishPercentileHistogram()
                        .register(meterRegistry));
    }
}
