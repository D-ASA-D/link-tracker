package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkUpdateNotificationService {

    private static final String UPDATE_HEADER = "Обновление по ссылке:\n\n";

    private final TelegramBot telegramBot;
    private final BotMetricsService metricsService;

    public void sendUpdate(ProcessedLinkUpdate update) {
        log.info(
                "processed_link_update_received id={} priority={} chats={}",
                update.id(),
                update.priority(),
                update.tgChatIds());

        for (Long chatId : update.tgChatIds()) {
            SendMessage message = new SendMessage(chatId, UPDATE_HEADER + update.description());

            SendResponse response = telegramBot.execute(message);

            if (response == null || !response.isOk()) {
                throw new IllegalStateException("Telegram message was not sent. chatId=" + chatId);
            }

            metricsService.incrementSentNotification();

            log.debug(
                    "processed_link_update_sent_to_telegram chatId={} id={} priority={}",
                    chatId,
                    update.id(),
                    update.priority());
        }
    }
}
