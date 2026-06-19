package backend.academy.linktracker.bot.dto;

public record DeleteLinksByTagResponse(String tag, int removedCount) {}
