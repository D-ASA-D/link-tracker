package backend.academy.linktracker.bot.dto;

public record RenameTagResponse(String oldName, String newName, int updatedSubscriptions) {}
