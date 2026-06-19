package backend.academy.linktracker.scrapper.metrics;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinksOnTrackMetricsBinder {

    private static final String GITHUB_DOMAIN = "github.com";
    private static final String STACKOVERFLOW_DOMAIN = "stackoverflow.com";

    private final MeterRegistry meterRegistry;
    private final LinkRepository linkRepository;

    @PostConstruct
    public void bind() {
        Gauge.builder(
                        "links_on_track_total",
                        linkRepository,
                        repository -> repository.countByUrlContaining(GITHUB_DOMAIN))
                .description("Number of tracked GitHub links")
                .tag("tracked_source", "github")
                .register(meterRegistry);

        Gauge.builder(
                        "links_on_track_total",
                        linkRepository,
                        repository -> repository.countByUrlContaining(STACKOVERFLOW_DOMAIN))
                .description("Number of tracked StackOverflow links")
                .tag("tracked_source", "stackoverflow")
                .register(meterRegistry);
    }
}
