package backend.academy.linktracker.bot.dto;

import java.util.List;

public record ProcessedLinkUpdate(Long id, String description, List<Long> tgChatIds, Priority priority) {}
