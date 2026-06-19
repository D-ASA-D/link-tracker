package backend.academy.linktracker.bot.dispatcher;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.state.SessionService;
import backend.academy.linktracker.bot.state.UserSession;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImplUpdateDispatcher implements UpdateDispatcher {

    private static final String TELEGRAM_COMMAND_SCOPE = "telegram_command";

    private final List<Command> commands;
    private final TelegramBot telegramBot;
    private final SessionService sessionService;
    private final ScrapperClient scrapperClient;
    private final BotMetricsService metricsService;

    @Override
    public void dispatch(Update update) {
        if (!isValidUpdate(update)) {
            return;
        }

        var text = update.message().text();

        if (isCommand(text)) {
            metricsService.incrementTelegramRequest("telegram_command");
        } else {
            metricsService.incrementTelegramRequest("telegram_text_message");
        }
        var chatId = update.message().chat().id();

        log.info("Received message. chatId={}, text={}", chatId, text);

        var session = sessionService.get(chatId);

        if (isCancelCommand(text)) {
            handleCancel(chatId);
            return;
        }

        if (isCommand(text)) {
            handleCommand(update, chatId, text, session);
            return;
        }

        handleStateTransition(chatId, text, session);
    }

    private boolean isValidUpdate(Update update) {
        if (update.message() == null || update.message().text() == null) {
            log.debug("Update without text message received: update={}", update);
            return false;
        }
        return true;
    }

    private boolean isCancelCommand(String text) {
        return "/cancel".equals(text);
    }

    private void handleCancel(Long chatId) {
        log.info("Cancel command received. chatId={}", chatId);
        sessionService.reset(chatId);
        log.debug("Session reset after cancel. chatId={}", chatId);
        telegramBot.execute(new SendMessage(chatId, "Процесс отменён."));
    }

    private boolean isCommand(String text) {
        return text.startsWith("/");
    }

    private void handleCommand(Update update, Long chatId, String text, UserSession session) {
        if (session.getState() != UserState.IDLE) {
            log.debug(
                    "Resetting session because new command arrived. chatId={}, previousState={}",
                    chatId,
                    session.getState());
            sessionService.reset(chatId);
        }

        String[] parts = text.split(" ", 2);
        String commandName = parts[0];
        String argument = parts.length > 1 ? parts[1] : null;

        metricsService.incrementCommandRequest(commandName);

        log.debug("Processing command. chatId={}, command={}, argument={}", chatId, commandName, argument);

        executeCommand(update, chatId, commandName, argument);
    }

    private void executeCommand(Update update, Long chatId, String commandName, String argument) {
        commands.stream()
                .filter(cmd -> cmd.name().equals(commandName))
                .findFirst()
                .ifPresentOrElse(
                        cmd -> {
                            log.debug("Executing command handler. chatId={}, command={}", chatId, commandName);

                            metricsService.recordCommandDuration(
                                    TELEGRAM_COMMAND_SCOPE, commandName, () -> cmd.handle(update, argument));
                        },
                        () -> handleUnknownCommand(chatId, commandName));
    }

    private void handleUnknownCommand(Long chatId, String commandName) {
        log.warn("Unknown command received. chatId={}, command={}", chatId, commandName);
        telegramBot.execute(new SendMessage(
                chatId, "Неизвестная команда. Используйте /help, чтобы узнать список доступных комманд."));
    }

    private void handleStateTransition(Long chatId, String text, UserSession session) {
        switch (session.getState()) {
            case WAITING_FOR_LINK -> handleWaitingForLink(chatId, text, session);
            case WAITING_FOR_TAGS -> handleWaitingForTags(chatId, text, session);
            case WAITING_FOR_UNTRACK -> handleWaitingForUntrack(chatId, text);
            default -> handleDefaultState(chatId, session);
        }
    }

    private void handleWaitingForLink(Long chatId, String text, UserSession session) {
        log.info("User sent link for tracking. chatId={}, url={}", chatId, text);

        if (!isValidUrl(text)) {
            log.warn("Invalid URL received. chatId={}, url={}", chatId, text);
            telegramBot.execute(new SendMessage(chatId, "Некорректная ссылка."));
            return;
        }

        session.setPendingUrl(text);
        session.setState(UserState.WAITING_FOR_TAGS);

        log.debug("Session state changed. chatId={}, newState={}", chatId, UserState.WAITING_FOR_TAGS);
        telegramBot.execute(new SendMessage(chatId, "Введите теги через запятую или отправьте '-'"));
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleWaitingForTags(Long chatId, String text, UserSession session) {
        log.info("User sent tags. chatId={}, rawTags={}", chatId, text);

        List<String> tags = parseTags(text);
        log.debug("Parsed tags. chatId={}, tags={}", chatId, tags);

        try {
            log.info("Adding link to scrapper. chatId={}, url={}, tags={}", chatId, session.getPendingUrl(), tags);

            scrapperClient.addLink(chatId, session.getPendingUrl(), tags);

            log.info("Link successfully added. chatId={}, url={}", chatId, session.getPendingUrl());
            telegramBot.execute(new SendMessage(chatId, "Ссылка добавлена."));

        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Link already tracked. chatId={}, url={}", chatId, session.getPendingUrl());
            telegramBot.execute(new SendMessage(chatId, "Ссылка уже отслеживается"));
        }

        sessionService.reset(chatId);
        log.debug("Session reset. chatId={}", chatId);
    }

    private List<String> parseTags(String text) {
        return text.equals("-")
                ? List.of()
                : java.util.Arrays.stream(text.split(",")).map(String::trim).toList();
    }

    private void handleWaitingForUntrack(Long chatId, String text) {
        log.info("User requested link removal. chatId={}, url={}", chatId, text);

        try {
            scrapperClient.removeLink(chatId, text);
            log.info("Link removed successfully. chatId={}, url={}", chatId, text);
            telegramBot.execute(new SendMessage(chatId, "Ссылка удалена."));

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Attempt to remove untracked link. chatId={}, url={}", chatId, text);
            telegramBot.execute(new SendMessage(chatId, "Ссылка не отслеживается."));
        }

        sessionService.reset(chatId);
        log.debug("Session reset. chatId={}", chatId);
    }

    private void handleDefaultState(Long chatId, UserSession session) {
        log.debug("No FSM action required. chatId={}, state={}", chatId, session.getState());
        telegramBot.execute(new SendMessage(
                chatId, "Неизвестная команда. Используйте /help, чтобы узнать список доступных комманд."));
    }
}
