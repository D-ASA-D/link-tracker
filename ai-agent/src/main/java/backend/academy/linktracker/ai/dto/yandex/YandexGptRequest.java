package backend.academy.linktracker.ai.dto.yandex;

import java.util.List;

public record YandexGptRequest(String modelUri, CompletionOptions completionOptions, List<Message> messages) {

    public record CompletionOptions(boolean stream, double temperature, int maxTokens) {}

    public record Message(String role, String text) {}
}
