package backend.academy.linktracker.bot.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.dto.Priority;
import backend.academy.linktracker.bot.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import backend.academy.linktracker.bot.service.ProcessedKafkaMessageService;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkUpdateKafkaListenerTest {

    private static final String TOPIC = "link.processed-updates";

    private LinkUpdateNotificationService notificationService;
    private ProcessedKafkaMessageService processedMessageService;
    private BotMetricsService metricsService;
    private LinkUpdateKafkaListener listener;

    @BeforeEach
    void setup() {
        notificationService = mock(LinkUpdateNotificationService.class);
        processedMessageService = mock(ProcessedKafkaMessageService.class);
        metricsService = mock(BotMetricsService.class);

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(2);
                    runnable.run();
                    return null;
                })
                .when(metricsService)
                .recordCommandDuration(anyString(), anyString(), any(Runnable.class));

        listener = new LinkUpdateKafkaListener(notificationService, processedMessageService, metricsService);
    }

    @Test
    void listen_shouldSkipMessage_whenRecordAlreadyProcessed() {
        ProcessedLinkUpdate update = new ProcessedLinkUpdate(1L, "test", List.of(123L), Priority.HIGH);

        ConsumerRecord<String, ProcessedLinkUpdate> record = new ConsumerRecord<>(TOPIC, 0, 10L, "key-1", update);

        when(processedMessageService.isProcessed(TOPIC, 0, 10L)).thenReturn(true);

        listener.listen(record);

        verify(notificationService, never()).sendUpdate(any());
        verify(processedMessageService, never()).markProcessed(anyString(), anyInt(), anyLong(), any());
    }

    @Test
    void listen_shouldSendUpdateAndMarkProcessed_whenRecordIsNew() {
        ProcessedLinkUpdate update = new ProcessedLinkUpdate(1L, "test", List.of(123L), Priority.HIGH);

        ConsumerRecord<String, ProcessedLinkUpdate> record = new ConsumerRecord<>(TOPIC, 0, 10L, "key-1", update);

        when(processedMessageService.isProcessed(TOPIC, 0, 10L)).thenReturn(false);

        listener.listen(record);

        verify(notificationService).sendUpdate(update);
        verify(processedMessageService).markProcessed(TOPIC, 0, 10L, "key-1");
    }

    @Test
    void listen_shouldNotMarkProcessed_whenNotificationServiceFails() {
        ProcessedLinkUpdate update = new ProcessedLinkUpdate(1L, "test", List.of(123L), Priority.HIGH);

        ConsumerRecord<String, ProcessedLinkUpdate> record = new ConsumerRecord<>(TOPIC, 0, 10L, "key-1", update);

        when(processedMessageService.isProcessed(TOPIC, 0, 10L)).thenReturn(false);

        doThrow(new RuntimeException("Telegram unavailable"))
                .when(notificationService)
                .sendUpdate(update);

        try {
            listener.listen(record);
        } catch (RuntimeException ignored) {
            // Expected: listener must not mark message as processed if sending failed.
        }

        verify(notificationService).sendUpdate(update);
        verify(processedMessageService, never()).markProcessed(anyString(), anyInt(), anyLong(), any());
    }
}
