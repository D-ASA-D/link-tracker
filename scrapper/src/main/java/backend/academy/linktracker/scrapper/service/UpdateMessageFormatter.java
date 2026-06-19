package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;

public interface UpdateMessageFormatter {
    String format(String url, LinkUpdateInfo info);
}
