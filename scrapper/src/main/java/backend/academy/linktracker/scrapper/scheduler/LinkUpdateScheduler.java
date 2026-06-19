package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class LinkUpdateScheduler {

    private final LinkUpdateService linkUpdateService;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        log.debug("scheduler_start");
        linkUpdateService.processDueLinks();
    }
}
