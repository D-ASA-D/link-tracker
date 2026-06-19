package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record RawLinkUpdate(Long id, String description, String author, List<Long> tgChatIds) {}
