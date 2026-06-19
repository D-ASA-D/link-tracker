package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.properties.BotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotClient {

    private static final String BOT_API_SCOPE = "bot_api";
    private static final String SEND_UPDATE_ENDPOINT = "POST /updates";

    private final RestTemplate restTemplate;
    private final BotProperties properties;
    private final ResilientHttpExecutor resilientHttpExecutor;
    private final ScrapperMetricsService metricsService;

    public void sendUpdate(RawLinkUpdate update) {
        log.info(
                "bot_raw_update_send linkId={} author={} chatsCount={}",
                update.id(),
                update.author(),
                update.tgChatIds().size());

        metricsService.recordRequestDuration(
                BOT_API_SCOPE,
                SEND_UPDATE_ENDPOINT,
                () -> resilientHttpExecutor.executeVoid(
                        () -> restTemplate.postForObject(properties.getBaseUrl() + "/updates", update, Void.class)));

        log.info("bot_raw_update_sent linkId={} author={}", update.id(), update.author());
    }
}
