package backend.academy.linktracker.scrapper.model.orm;

import java.io.Serializable;

public record SubscriptionTagId(Long subscriptionId, Long tagId) implements Serializable {}
