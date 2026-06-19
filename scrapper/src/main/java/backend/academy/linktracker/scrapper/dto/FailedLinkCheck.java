package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record FailedLinkCheck(Long linkId, String url, String reason, List<Long> chatIds) {}
