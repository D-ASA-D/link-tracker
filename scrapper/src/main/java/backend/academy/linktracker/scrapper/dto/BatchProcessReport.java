package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record BatchProcessReport(int processed, int succeeded, int failed, List<FailedLinkCheck> failedLinks) {}
