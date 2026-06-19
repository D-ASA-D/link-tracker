package backend.academy.linktracker.bot.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameTagRequest(
        @NotBlank String oldName, @NotBlank String newName) {}
