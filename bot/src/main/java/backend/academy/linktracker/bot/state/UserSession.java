package backend.academy.linktracker.bot.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSession {

    private UserState state = UserState.IDLE;
    private String pendingUrl;
}
