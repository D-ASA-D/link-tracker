package backend.academy.linktracker.scrapper.outbox;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED
}
