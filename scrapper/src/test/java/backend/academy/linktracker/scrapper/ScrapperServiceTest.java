package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetricsService;
import backend.academy.linktracker.scrapper.model.LinkRecord;
import backend.academy.linktracker.scrapper.model.SubscriptionRecord;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import backend.academy.linktracker.scrapper.service.ScrapperService;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScrapperServiceTest {

    private ChatRepository chatRepository;
    private LinkRepository linkRepository;
    private SubscriptionRepository subscriptionRepository;
    private TagRepository tagRepository;
    private ScrapperMetricsService metricsService;

    private ScrapperService service;

    @BeforeEach
    void setup() {
        chatRepository = mock(ChatRepository.class);
        linkRepository = mock(LinkRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);
        tagRepository = mock(TagRepository.class);
        metricsService = mock(ScrapperMetricsService.class);

        mockMetricsService(metricsService);

        service = new ScrapperService(
                linkRepository, chatRepository, subscriptionRepository, tagRepository, metricsService);
    }

    @Test
    void registerChat_success() {
        assertDoesNotThrow(() -> service.registerChat(1L));

        verify(chatRepository).register(1L);
    }

    @Test
    void registerChat_alreadyExists() {
        doThrow(new IllegalStateException("Chat already exists"))
                .when(chatRepository)
                .register(1L);

        assertThrows(IllegalStateException.class, () -> service.registerChat(1L));
    }

    @Test
    void addLink_existingLink_createsSubscription() {
        when(chatRepository.exists(1L)).thenReturn(true);

        LinkRecord link = new LinkRecord(
                10L, "http://test.com", Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-01T00:10:00Z"));

        when(linkRepository.findByUrl("http://test.com")).thenReturn(Optional.of(link));
        when(subscriptionRepository.existsByChatIdAndLinkId(1L, 10L)).thenReturn(false);
        when(subscriptionRepository.save(1L, 10L)).thenReturn(new SubscriptionRecord(100L, 1L, 10L));
        when(tagRepository.findBySubscriptionId(100L)).thenReturn(List.of());

        AddLinkRequest request = new AddLinkRequest("http://test.com", List.of(), List.of());

        service.addLink(1L, request);

        verify(chatRepository).exists(1L);
        verify(linkRepository).findByUrl("http://test.com");
        verify(subscriptionRepository).existsByChatIdAndLinkId(1L, 10L);
        verify(subscriptionRepository).save(1L, 10L);
        verify(tagRepository).findBySubscriptionId(100L);
    }

    @Test
    void addLink_missingChat_shouldThrow() {
        when(chatRepository.exists(1L)).thenReturn(false);

        AddLinkRequest request = new AddLinkRequest("http://test.com", List.of(), List.of());

        assertThrows(NoSuchElementException.class, () -> service.addLink(1L, request));

        verify(chatRepository).exists(1L);
    }

    @Test
    void removeLink_deletesSubscriptionAndLinkIfNoSubscribersLeft() {
        when(chatRepository.exists(1L)).thenReturn(true);

        LinkRecord link = new LinkRecord(
                10L, "http://test.com", Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-01-01T00:10:00Z"));

        SubscriptionRecord subscription = new SubscriptionRecord(100L, 1L, 10L);

        when(linkRepository.findByUrl("http://test.com")).thenReturn(Optional.of(link));
        when(subscriptionRepository.findByChatIdAndLinkId(1L, 10L)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.countByLinkId(10L)).thenReturn(0L);
        when(tagRepository.findBySubscriptionId(100L)).thenReturn(List.of());

        RemoveLinkRequest request = new RemoveLinkRequest("http://test.com");

        service.removeLink(1L, request);

        verify(chatRepository).exists(1L);
        verify(linkRepository).findByUrl("http://test.com");
        verify(subscriptionRepository).findByChatIdAndLinkId(1L, 10L);
        verify(tagRepository).findBySubscriptionId(100L);
        verify(tagRepository).detachAllFromSubscription(100L);
        verify(subscriptionRepository).deleteById(100L);
        verify(subscriptionRepository).countByLinkId(10L);
        verify(linkRepository).deleteById(10L);
    }

    @Test
    void listLinks_chatNotFound() {
        when(chatRepository.exists(1L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> service.listLinks(1L));

        verify(chatRepository).exists(1L);
    }

    private void mockMetricsService(ScrapperMetricsService metricsService) {
        when(metricsService.recordRequestDuration(anyString(), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });

        doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(2);
                    runnable.run();
                    return null;
                })
                .when(metricsService)
                .recordRequestDuration(anyString(), anyString(), any(Runnable.class));
    }
}
