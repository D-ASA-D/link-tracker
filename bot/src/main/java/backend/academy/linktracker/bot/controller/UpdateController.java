package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetricsService;
import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UpdateController {

    private static final String SCRAPPER_SYNC_API_SCOPE = "scrapper_sync_api";
    private static final String HTTP_UPDATES_SCOPE_TYPE = "httpUpdates";

    private final LinkUpdateNotificationService notificationService;
    private final BotMetricsService metricsService;

    @PostMapping("/updates")
    public ResponseEntity<Void> update(@RequestBody ProcessedLinkUpdate update) {
        metricsService.recordCommandDuration(
                SCRAPPER_SYNC_API_SCOPE, HTTP_UPDATES_SCOPE_TYPE, () -> notificationService.sendUpdate(update));

        return ResponseEntity.ok().build();
    }
}
