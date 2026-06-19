package backend.academy.linktracker.scrapper.dto;

public record DeleteLinksByTagResponse(String tag, int removedCount) {}
