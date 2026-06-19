package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.cache.LinksListCache;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.DeleteLinksByTagResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.CachedScrapperService;
import backend.academy.linktracker.scrapper.service.ScrapperService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedScrapperServiceTest {

    private static final Long CHAT_ID = 805052108L;
    private static final int DEFAULT_LIMIT = 100;
    private static final int DEFAULT_OFFSET = 0;

    private ScrapperService scrapperService;
    private LinksListCache linksListCache;
    private CachedScrapperService cachedScrapperService;

    @BeforeEach
    void setUp() {
        scrapperService = mock(ScrapperService.class);
        linksListCache = mock(LinksListCache.class);
        cachedScrapperService = new CachedScrapperService(scrapperService, linksListCache);
    }

    @Test
    void shouldCallScrapperServiceAndPutResponseToCacheWhenCacheMiss() {
        ListLinksResponse response = mock(ListLinksResponse.class);

        when(linksListCache.get(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(Optional.empty());
        when(scrapperService.listLinks(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(response);

        ListLinksResponse result = cachedScrapperService.listLinks(CHAT_ID);

        assertSame(response, result);

        verify(linksListCache).get(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET);
        verify(scrapperService).listLinks(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET);
        verify(linksListCache).put(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET, response);
    }

    @Test
    void shouldReturnCachedResponseAndNotCallScrapperServiceWhenCacheHit() {
        ListLinksResponse response = mock(ListLinksResponse.class);

        when(linksListCache.get(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET)).thenReturn(Optional.of(response));

        ListLinksResponse result = cachedScrapperService.listLinks(CHAT_ID);

        assertSame(response, result);

        verify(linksListCache).get(CHAT_ID, DEFAULT_LIMIT, DEFAULT_OFFSET);
        verifyNoInteractions(scrapperService);
        verify(linksListCache, never()).put(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void shouldEvictCacheAfterAddLink() {
        AddLinkRequest request = mock(AddLinkRequest.class);
        LinkResponse response = mock(LinkResponse.class);

        when(scrapperService.addLink(CHAT_ID, request)).thenReturn(response);

        LinkResponse result = cachedScrapperService.addLink(CHAT_ID, request);

        assertSame(response, result);

        verify(scrapperService).addLink(CHAT_ID, request);
        verify(linksListCache).evict(CHAT_ID);
    }

    @Test
    void shouldEvictCacheAfterRemoveLink() {
        RemoveLinkRequest request = mock(RemoveLinkRequest.class);
        LinkResponse response = mock(LinkResponse.class);

        when(scrapperService.removeLink(CHAT_ID, request)).thenReturn(response);

        LinkResponse result = cachedScrapperService.removeLink(CHAT_ID, request);

        assertSame(response, result);

        verify(scrapperService).removeLink(CHAT_ID, request);
        verify(linksListCache).evict(CHAT_ID);
    }

    @Test
    void shouldEvictCacheAfterRemoveLinksByTag() {
        String tag = "java";
        DeleteLinksByTagResponse response = mock(DeleteLinksByTagResponse.class);

        when(scrapperService.removeLinksByTag(CHAT_ID, tag)).thenReturn(response);

        DeleteLinksByTagResponse result = cachedScrapperService.removeLinksByTag(CHAT_ID, tag);

        assertSame(response, result);

        verify(scrapperService).removeLinksByTag(CHAT_ID, tag);
        verify(linksListCache).evict(CHAT_ID);
    }

    @Test
    void shouldEvictCacheAfterUnregisterChat() {
        cachedScrapperService.unregisterChat(CHAT_ID);

        verify(scrapperService).unregisterChat(CHAT_ID);
        verify(linksListCache).evict(CHAT_ID);
    }
}
