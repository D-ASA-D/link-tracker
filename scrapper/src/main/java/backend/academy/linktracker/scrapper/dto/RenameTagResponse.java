package backend.academy.linktracker.scrapper.dto;

public record RenameTagResponse(String oldName, String newName, int updatedSubscriptions) {}
