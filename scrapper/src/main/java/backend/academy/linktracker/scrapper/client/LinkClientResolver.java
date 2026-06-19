package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.dto.LinkUpdateInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkClientResolver {

    private final GithubClient githubClient;
    private final StackoverflowClient stackoverflowClient;

    public Optional<LinkUpdateInfo> resolve(String url) {
        log.debug("resolve_link url={}", url);

        if (url.contains("github.com")) {
            return githubClient.getLastUpdate(url);
        }

        if (url.contains("stackoverflow.com")) {
            return stackoverflowClient.getLastUpdate(url);
        }

        log.warn("unsupported_link url={}", url);
        throw new IllegalArgumentException("Unsupported link: " + url);
    }
}
