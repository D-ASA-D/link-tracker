package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.outbox.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "HTTP_WITH_KAFKA_FALLBACK")
public class FallbackUpdateSender implements UpdateSender {

    private final BotClient botClient;
    private final NotificationOutboxRepository outboxRepository;

    @Override
    public void send(RawLinkUpdate update) {
        try {
            botClient.sendUpdate(update);
            log.info("raw_update_sent_by_http linkId={} author={}", update.id(), update.author());
        } catch (Exception exception) {
            log.warn(
                    "http_raw_update_failed_fallback_to_outbox linkId={} author={}",
                    update.id(),
                    update.author(),
                    exception);

            outboxRepository.save(update);

            log.info(
                    "fallback_raw_outbox_event_saved linkId={} author={} chatsCount={}",
                    update.id(),
                    update.author(),
                    update.tgChatIds().size());
        }
    }
}
