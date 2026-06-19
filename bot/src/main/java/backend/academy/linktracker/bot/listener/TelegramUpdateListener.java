package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.dispatcher.UpdateDispatcher;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateListener {

    private final TelegramBot telegramBot;

    private final UpdateDispatcher dispatcher;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this::process);

        telegramBot.execute(new SetMyCommands(
                new BotCommand("/start", "Запуск бота"),
                new BotCommand("/help", "Список команд"),
                new BotCommand("/track", "Добавить ссылку"),
                new BotCommand("/untrack", "Удалить ссылку"),
                new BotCommand("/list", "Список ссылок"),
                new BotCommand("/cancel", "Отменить текущую операцию"),
                new BotCommand("/tag_rename", "Переименовать тег"),
                new BotCommand("/untag_links", "Удалить все ссылки по тегу")));

        log.info("Telegram commands have been created.");
    }

    private int process(List<Update> updateList) {
        log.debug("Received updates from Telegram. count={}", updateList.size());
        for (Update update : updateList) {
            if (update.message() != null) {
                dispatcher.dispatch(update);
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
