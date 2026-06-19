package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notification", name = "transport", havingValue = "HTTP")
public class HttpUpdateSender implements UpdateSender {

    private final BotClient botClient;

    @Override
    public void send(RawLinkUpdate update) {
        botClient.sendUpdate(update);
    }
}
