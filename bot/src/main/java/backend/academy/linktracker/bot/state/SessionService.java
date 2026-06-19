package backend.academy.linktracker.bot.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SessionService {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession get(Long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new UserSession());
    }

    public void reset(Long chatId) {
        sessions.remove(chatId);
    }
}
