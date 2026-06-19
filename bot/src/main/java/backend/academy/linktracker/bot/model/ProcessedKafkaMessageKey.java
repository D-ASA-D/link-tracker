package backend.academy.linktracker.bot.model;

public record ProcessedKafkaMessageKey(String topic, int partition, long offset) {}
