package backend.academy.linktracker.ai.dto;

import java.util.List;

public record RawLinkUpdate(Long id, String description, String author, List<Long> tgChatIds) {}
