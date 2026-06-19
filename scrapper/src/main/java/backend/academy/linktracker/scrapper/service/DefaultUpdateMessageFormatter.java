package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import org.springframework.stereotype.Service;

@Service
public class DefaultUpdateMessageFormatter implements UpdateMessageFormatter {

    @Override
    public String format(String url, LinkUpdateInfo info) {
        return ("Обновление по ссылке: %s%n" + "Тип: %s%n"
                        + "Заголовок: %s%n"
                        + "Пользователь: %s%n"
                        + "Время создания: %s%n"
                        + "Превью: %s")
                .formatted(
                        url,
                        safe(info.eventType()),
                        safe(info.title()),
                        safe(info.username()),
                        info.createdAt(),
                        safe(info.preview()));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
