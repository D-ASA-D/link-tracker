package backend.academy.linktracker.ai.dto.yandex;

import java.util.List;

public record YandexGptResponse(Result result) {

    public record Result(List<Alternative> alternatives) {}

    public record Alternative(Message message) {}

    public record Message(String role, String text) {}
}
