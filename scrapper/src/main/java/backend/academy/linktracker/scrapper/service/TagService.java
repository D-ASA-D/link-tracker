package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.TagRecord;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final String DATABASE_SCOPE = "database";
    private static final String TAGS_TABLE = "tags";

    private final TagRepository tagRepository;
    private final ScrapperMetricsService metricsService;

    @Transactional
    public TagRecord createTag(String name) {
        String normalized = normalize(name);
        return database(() -> tagRepository.findOrCreate(normalized));
    }

    public TagRecord getTag(Long id) {
        return database(() -> tagRepository.findById(id))
                .orElseThrow(() -> new NoSuchElementException("Tag not found"));
    }

    public List<TagRecord> listTags(int limit, int offset) {
        return database(() -> tagRepository.findAll(limit, offset));
    }

    @Transactional
    public TagRecord updateTag(Long id, String name) {
        String normalized = normalize(name);

        if (database(() -> tagRepository.findById(id)).isEmpty()) {
            throw new NoSuchElementException("Tag not found");
        }

        return database(() -> tagRepository.update(id, normalized));
    }

    @Transactional
    public void deleteTag(Long id) {
        if (database(() -> tagRepository.findById(id)).isEmpty()) {
            throw new NoSuchElementException("Tag not found");
        }

        database(() -> tagRepository.deleteById(id));
    }

    private String normalize(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Tag name must not be null");
        }
        String normalized = name.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank");
        }
        return normalized;
    }

    private <T> T database(Supplier<T> action) {
        return metricsService.recordRequestDuration(DATABASE_SCOPE, TAGS_TABLE, action);
    }

    private void database(Runnable action) {
        metricsService.recordRequestDuration(DATABASE_SCOPE, TAGS_TABLE, action);
    }
}
