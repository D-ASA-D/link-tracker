package backend.academy.linktracker.bot.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotMetricsService {

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> commandCounters = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> commandDurationMetrics = new ConcurrentHashMap<>();
    private final Map<String, Counter> telegramRequestCounters = new ConcurrentHashMap<>();

    private Counter sentNotificationCounter;

    @PostConstruct
    public void init() {
        sentNotificationCounter = Counter.builder("sent_notification")
                .description("Count of sent Telegram notifications")
                .register(meterRegistry);
    }

    public void incrementTelegramRequest(String requestType) {
        telegramRequestCounters
                .computeIfAbsent(requestType, this::createTelegramRequestCounter)
                .increment();
    }

    private Counter createTelegramRequestCounter(String requestType) {
        return Counter.builder("telegram_requests")
                .description("Count of incoming Telegram requests")
                .tag("request_type", requestType)
                .register(meterRegistry);
    }

    public void incrementCommandRequest(String command) {
        commandCounters.computeIfAbsent(command, this::createCommandCounter).increment();
    }

    public <T> T recordCommandDuration(String scope, String scopeType, Supplier<T> action) {
        long startedAt = System.nanoTime();

        try {
            return action.get();
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            commandDurationMetric(scope, scopeType).record(durationMs);
        }
    }

    public void recordCommandDuration(String scope, String scopeType, Runnable action) {
        long startedAt = System.nanoTime();

        try {
            action.run();
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            commandDurationMetric(scope, scopeType).record(durationMs);
        }
    }

    public void incrementSentNotification() {
        sentNotificationCounter.increment();
    }

    private Counter createCommandCounter(String command) {
        return Counter.builder("command_requests")
                .description("Count of processed Bot commands")
                .tag("command", command)
                .tag("request_type", "telegram_command")
                .register(meterRegistry);
    }

    private DistributionSummary commandDurationMetric(String scope, String scopeType) {
        String key = scope + ":" + scopeType;

        return commandDurationMetrics.computeIfAbsent(
                key, ignored -> DistributionSummary.builder("command_duration_ms_total")
                        .description("Duration of Bot command operations in milliseconds")
                        .baseUnit("milliseconds")
                        .tag("scope", scope)
                        .tag("scope_type", scopeType)
                        .serviceLevelObjectives(10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000)
                        .publishPercentileHistogram()
                        .register(meterRegistry));
    }
}
