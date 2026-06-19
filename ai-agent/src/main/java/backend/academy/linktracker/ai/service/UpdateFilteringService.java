package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateFilteringService {

    private final AiAgentProperties properties;

    public boolean shouldProcess(RawLinkUpdate update) {
        return !containsStopWord(update) && !hasExcludedAuthor(update) && !isTooShort(update);
    }

    private boolean containsStopWord(RawLinkUpdate update) {
        String description = normalize(update.description());

        return properties.getFilter().getStopWords().stream()
                .map(this::normalize)
                .anyMatch(description::contains);
    }

    private boolean hasExcludedAuthor(RawLinkUpdate update) {
        String author = normalize(update.author());

        return properties.getFilter().getExcludedAuthors().stream()
                .map(this::normalize)
                .anyMatch(author::equals);
    }

    private boolean isTooShort(RawLinkUpdate update) {
        String description = update.description();

        if (description == null) {
            return true;
        }

        return description.length() < properties.getFilter().getMinLength();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }
}
