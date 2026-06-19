package backend.academy.linktracker.scrapper.dto;

import java.time.Instant;

public record LinkUpdateInfo(
        Instant eventTime, String title, String username, Instant createdAt, String preview, String eventType) {}
