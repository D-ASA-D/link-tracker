package backend.academy.linktracker.bot.dispatcher;

import com.pengrad.telegrambot.model.Update;

public interface UpdateDispatcher {

    void dispatch(Update update);
}
