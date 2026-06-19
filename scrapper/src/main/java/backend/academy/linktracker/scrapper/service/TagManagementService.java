package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.RenameTagRequest;
import backend.academy.linktracker.scrapper.dto.RenameTagResponse;

public interface TagManagementService {
    RenameTagResponse renameTag(Long chatId, RenameTagRequest request);
}
