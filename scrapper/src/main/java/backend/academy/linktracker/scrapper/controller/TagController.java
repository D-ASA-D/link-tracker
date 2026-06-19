package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.RenameTagRequest;
import backend.academy.linktracker.scrapper.dto.RenameTagResponse;
import backend.academy.linktracker.scrapper.service.TagManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/tags")
public class TagController {

    private static final String TG_CHAT_ID = "Tg-Chat-Id";

    private final TagManagementService tagManagementService;

    @PutMapping
    public RenameTagResponse renameTag(
            @RequestHeader(TG_CHAT_ID) Long chatId, @Valid @RequestBody RenameTagRequest request) {
        return tagManagementService.renameTag(chatId, request);
    }
}
