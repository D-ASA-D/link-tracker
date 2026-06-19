package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.Priority;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrioritizationService {

    private final AiAgentProperties properties;

    public Priority definePriority(String description) {
        String normalizedDescription = normalize(description);

        boolean hasHighKeywords = properties.getPrioritization().getHighKeywords().stream()
                .map(this::normalize)
                .anyMatch(normalizedDescription::contains);

        if (hasHighKeywords) {
            return Priority.HIGH;
        }

        boolean hasLowKeywords = properties.getPrioritization().getLowKeywords().stream()
                .map(this::normalize)
                .anyMatch(normalizedDescription::contains);

        if (hasLowKeywords) {
            return Priority.LOW;
        }

        return Priority.MEDIUM;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase().trim();
    }
}
