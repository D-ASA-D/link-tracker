package backend.academy.linktracker.scrapper.repository;

public interface ChatRepository {
    void register(Long chatId);

    void unregister(Long chatId);

    boolean exists(Long chatId);
}
