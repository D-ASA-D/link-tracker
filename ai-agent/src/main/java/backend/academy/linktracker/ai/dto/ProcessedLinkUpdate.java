package backend.academy.linktracker.ai.dto;

import java.util.List;

public record ProcessedLinkUpdate(Long id, String description, List<Long> tgChatIds, Priority priority) {}
