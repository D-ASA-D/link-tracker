package backend.academy.linktracker.scrapper.model;

import java.time.Instant;

public record LinkRecord(Long id, String url, Instant lastUpdated, Instant lastCheckedAt) {}
