package backend.academy.linktracker.scrapper.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.service.HttpUpdateSender;
import backend.academy.linktracker.scrapper.service.KafkaUpdateSender;
import backend.academy.linktracker.scrapper.service.OutboxUpdateSender;
import backend.academy.linktracker.scrapper.service.UpdateSender;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotificationTransportConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestNotificationConfiguration.class)
            .withBean(ScrapperMetricsService.class, this::mockMetricsService);

    @Test
    void shouldUseHttpUpdateSender_whenTransportIsHttp() {
        contextRunner.withPropertyValues("app.notification.transport=HTTP").run(context -> {
            assertThat(context).hasSingleBean(UpdateSender.class);
            assertThat(context).hasSingleBean(HttpUpdateSender.class);
            assertThat(context).doesNotHaveBean(KafkaUpdateSender.class);
            assertThat(context).doesNotHaveBean(OutboxUpdateSender.class);
        });
    }

    @Test
    void shouldUseKafkaUpdateSender_whenTransportIsKafka() {
        contextRunner.withPropertyValues("app.notification.transport=KAFKA").run(context -> {
            assertThat(context).hasSingleBean(UpdateSender.class);
            assertThat(context).hasSingleBean(KafkaUpdateSender.class);
            assertThat(context).doesNotHaveBean(HttpUpdateSender.class);
            assertThat(context).doesNotHaveBean(OutboxUpdateSender.class);
        });
    }

    @Test
    void shouldUseOutboxUpdateSender_whenTransportIsOutbox() {
        contextRunner.withPropertyValues("app.notification.transport=OUTBOX").run(context -> {
            assertThat(context).hasSingleBean(UpdateSender.class);
            assertThat(context).hasSingleBean(OutboxUpdateSender.class);
            assertThat(context).doesNotHaveBean(HttpUpdateSender.class);
            assertThat(context).doesNotHaveBean(KafkaUpdateSender.class);
        });
    }

    @Test
    void shouldUseKafkaUpdateSender_whenTransportIsMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(UpdateSender.class);
            assertThat(context).hasSingleBean(KafkaUpdateSender.class);
            assertThat(context).doesNotHaveBean(HttpUpdateSender.class);
            assertThat(context).doesNotHaveBean(OutboxUpdateSender.class);
        });
    }

    private ScrapperMetricsService mockMetricsService() {
        ScrapperMetricsService metricsService = mock(ScrapperMetricsService.class);

        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(2);
                    runnable.run();
                    return null;
                })
                .when(metricsService)
                .recordRequestDuration(anyString(), anyString(), any(Runnable.class));

        return metricsService;
    }
}
