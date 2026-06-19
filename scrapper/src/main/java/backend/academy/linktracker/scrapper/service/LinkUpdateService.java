package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.BatchProcessReport;

public interface LinkUpdateService {
    BatchProcessReport processDueLinks();
}
