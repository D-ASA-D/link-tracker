package backend.academy.linktracker.bot.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.state.SessionService;
import backend.academy.linktracker.bot.state.UserSession;
import backend.academy.linktracker.bot.state.UserState;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ImplUpdateDispatcherTest {

    private TelegramBot telegramBot;
    private Command startCommand;
    private Command helpCommand;
    private ImplUpdateDispatcher dispatcher;

    private SessionService sessionService;
    private ScrapperClient scrapperClient;
    private BotMetricsService metricsService;

    @BeforeEach
    void setUp() {

        telegramBot = mock(TelegramBot.class);
        startCommand = mock(Command.class);
        helpCommand = mock(Command.class);
        sessionService = mock(SessionService.class);
        scrapperClient = mock(ScrapperClient.class);
        metricsService = mock(BotMetricsService.class);
        mockMetricsService();

        when(startCommand.name()).thenReturn("/start");
        when(helpCommand.name()).thenReturn("/help");

        dispatcher = new ImplUpdateDispatcher(
                List.of(startCommand, helpCommand), telegramBot, sessionService, scrapperClient, metricsService);

        when(sessionService.get(1L)).thenReturn(new UserSession());
    }

    @Test
    void shouldDisplayStartCommand() {
        Update update = mockUpdate("/start");
        dispatcher.dispatch(update);
        verify(startCommand).handle(update, null);
        verifyNoInteractions(telegramBot);
    }

    @Test
    void shouldDisplayHelpCommand() {
        Update update = mockUpdate("/help");
        dispatcher.dispatch(update);
        verify(helpCommand).handle(update, null);
        verifyNoInteractions(telegramBot);
    }

    @Test
    void shouldReturnErrorForUnknownCommand() {
        Update update = mockUpdate("/qwertd");
        dispatcher.dispatch(update);
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        SendMessage message = captor.getValue();
        assertThat(message.getParameters().get("text"))
                .isEqualTo("Неизвестная команда. Используйте /help, чтобы узнать список доступных комманд.");
    }

    @Test
    void shouldReturnErrorForTextWithoutSlash() {
        Update update = mockUpdate("qwertd");
        dispatcher.dispatch(update);
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        SendMessage message = captor.getValue();
        assertThat(message.getParameters().get("text"))
                .isEqualTo("Неизвестная команда. Используйте /help, чтобы узнать список доступных комманд.");
    }

    @Test
    void shouldRejectInvalidUrl() {

        UserSession session = new UserSession();
        session.setState(UserState.WAITING_FOR_LINK);

        when(sessionService.get(1L)).thenReturn(session);

        Update update = mockUpdate("invalid-url");

        dispatcher.dispatch(update);

        verify(telegramBot).execute(Mockito.any(SendMessage.class));
    }

    @Test
    void trackCommandFullFlow() {

        TelegramBot bot = mock(TelegramBot.class);
        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        SessionService sessionService = new SessionService();
        BotMetricsService metricsService = mock(BotMetricsService.class);
        mockMetricsService(metricsService);

        Command trackCommand = mock(Command.class);

        when(trackCommand.name()).thenReturn("/track");
        Mockito.doAnswer(invocation -> {
                    Update update = invocation.getArgument(0);
                    Long chatId = update.message().chat().id();

                    sessionService.get(chatId).setState(UserState.WAITING_FOR_LINK);
                    return null;
                })
                .when(trackCommand)
                .handle(Mockito.any(), Mockito.any());

        ImplUpdateDispatcher dispatcher =
                new ImplUpdateDispatcher(List.of(trackCommand), bot, sessionService, scrapperClient, metricsService);

        Long chatId = 1L;

        Update update = mockUpdate("/track");

        dispatcher.dispatch(update);

        assertThat(sessionService.get(chatId).getState()).isEqualTo(UserState.WAITING_FOR_LINK);
    }

    @Test
    void cancelResetsFsmState() {
        TelegramBot bot = mock(TelegramBot.class);
        SessionService sessionService = new SessionService();
        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        BotMetricsService metricsService = mock(BotMetricsService.class);
        mockMetricsService(metricsService);

        ImplUpdateDispatcher dispatcher =
                new ImplUpdateDispatcher(List.<Command>of(), bot, sessionService, scrapperClient, metricsService);

        Long chatId = 1L;
        sessionService.get(chatId).setState(UserState.WAITING_FOR_TAGS);

        Update cancelUpdate = mockUpdate("/cancel");

        dispatcher.dispatch(cancelUpdate);

        assertThat(sessionService.get(chatId).getState()).isEqualTo(UserState.IDLE);
        verify(bot).execute(Mockito.any(SendMessage.class));
    }

    @Test
    void invalidUrlWhileTracking() {
        TelegramBot bot = mock(TelegramBot.class);
        ScrapperClient scrapperClient = mock(ScrapperClient.class);
        SessionService sessionService = new SessionService();
        BotMetricsService metricsService = mock(BotMetricsService.class);
        mockMetricsService(metricsService);

        ImplUpdateDispatcher dispatcher =
                new ImplUpdateDispatcher(List.<Command>of(), bot, sessionService, scrapperClient, metricsService);

        Long chatId = 1L;
        sessionService.get(chatId).setState(UserState.WAITING_FOR_LINK);

        Update invalidUrlUpdate = mockUpdate("invalid-url");

        dispatcher.dispatch(invalidUrlUpdate);

        verify(bot)
                .execute(argThat(sendMsg -> sendMsg.getParameters().get("text").equals("Некорректная ссылка.")));
    }

    private void mockMetricsService() {
        mockMetricsService(this.metricsService);
    }

    private void mockMetricsService(BotMetricsService metricsService) {
        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(2);
                    runnable.run();
                    return null;
                })
                .when(metricsService)
                .recordCommandDuration(anyString(), anyString(), any(Runnable.class));
    }

    private Update mockUpdate(String text) {

        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn((long) 1);

        return update;
    }
}
