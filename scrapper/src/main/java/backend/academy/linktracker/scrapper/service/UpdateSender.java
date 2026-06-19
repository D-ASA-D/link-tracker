package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.RawLinkUpdate;

public interface UpdateSender {
    void send(RawLinkUpdate update);
}
