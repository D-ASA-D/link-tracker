package backend.academy.linktracker.scrapper.properties;

public enum NotificationTransport {
    HTTP,
    KAFKA,
    OUTBOX,
    HTTP_WITH_KAFKA_FALLBACK
}
