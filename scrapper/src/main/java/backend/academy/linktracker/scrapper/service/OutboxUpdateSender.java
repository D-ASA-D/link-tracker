package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.outbox.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "OUTBOX")
public class OutboxUpdateSender implements UpdateSender {

    private final NotificationOutboxRepository outboxRepository;

    @Override
    public void send(RawLinkUpdate update) {
        outboxRepository.save(update);

        log.info(
                "outbox_raw_event_saved linkId={} author={} chatsCount={}",
                update.id(),
                update.author(),
                update.tgChatIds().size());
    }
}
