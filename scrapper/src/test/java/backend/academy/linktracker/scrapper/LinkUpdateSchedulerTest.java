package backend.academy.linktracker.scrapper;

import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.scheduler.LinkUpdateScheduler;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkUpdateSchedulerTest {

    private LinkUpdateService linkUpdateService;
    private LinkUpdateScheduler scheduler;

    @BeforeEach
    void setup() {
        linkUpdateService = mock(LinkUpdateService.class);
        scheduler = new LinkUpdateScheduler(linkUpdateService);
    }

    @Test
    void checkUpdates_shouldDelegateToLinkUpdateService() {
        scheduler.checkUpdates();

        verify(linkUpdateService).processDueLinks();
    }
}
