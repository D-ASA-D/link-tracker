package backend.academy.linktracker.scrapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import backend.academy.linktracker.scrapper.controller.ScrapperController;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.CachedScrapperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScrapperControllerTest {

    private MockMvc mockMvc;
    private CachedScrapperService service;
    // private ScrapperService service;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        service = mock(CachedScrapperService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ScrapperController(service))
                .setControllerAdvice(new backend.academy.linktracker.scrapper.controller.GlobalExceptionHandler())
                .build();
        mapper = new ObjectMapper();
    }

    @Test
    void registerChat_ok() throws Exception {
        doNothing().when(service).registerChat(1L);

        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        verify(service).registerChat(1L);
    }

    @Test
    void registerChat_conflict() throws Exception {
        doThrow(new IllegalStateException("Chat already exists")).when(service).registerChat(1L);

        mockMvc.perform(post("/tg-chat/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"))
                .andExpect(jsonPath("$.exceptionName").value("IllegalStateException"));
    }

    @Test
    void unregisterChat_ok() throws Exception {
        doNothing().when(service).unregisterChat(1L);

        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());

        verify(service).unregisterChat(1L);
    }

    @Test
    void unregisterChat_notFound() throws Exception {
        doThrow(new NoSuchElementException("Chat not found")).when(service).unregisterChat(1L);

        mockMvc.perform(delete("/tg-chat/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.exceptionName").value("NoSuchElementException"));
    }

    @Test
    void listLinks_ok() throws Exception {
        ListLinksResponse response = new ListLinksResponse(
                List.of(new LinkResponse(1L, "http://test.com", List.of("tag"), List.of("filt"))), 1);

        when(service.listLinks(1L)).thenReturn(response);

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].id").value(1L));

        verify(service).listLinks(1L);
    }

    @Test
    void listLinks_notFound() throws Exception {
        when(service.listLinks(1L)).thenThrow(new NoSuchElementException("Chat not found"));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    void addLink_ok() throws Exception {
        AddLinkRequest request = new AddLinkRequest("http://test.com", List.of("tag"), List.of("filt"));
        LinkResponse resp = new LinkResponse(1L, "http://test.com", List.of("tag"), List.of("filt"));

        when(service.addLink(1L, request)).thenReturn(resp);

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.url").value("http://test.com"));

        verify(service).addLink(1L, request);
    }

    @Test
    void addLink_conflict() throws Exception {
        AddLinkRequest request = new AddLinkRequest("http://test.com", null, null);
        when(service.addLink(1L, request)).thenThrow(new IllegalStateException("Link already tracked"));

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("409"));
    }

    @Test
    void removeLink_ok() throws Exception {
        RemoveLinkRequest request = new RemoveLinkRequest("http://test.com");
        LinkResponse resp = new LinkResponse(1L, "http://test.com", List.of("tag"), List.of("filt"));

        when(service.removeLink(1L, request)).thenReturn(resp);

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).removeLink(1L, request);
    }

    @Test
    void removeLink_notFound() throws Exception {
        RemoveLinkRequest request = new RemoveLinkRequest("http://test.com");
        when(service.removeLink(1L, request)).thenThrow(new NoSuchElementException("Link not found"));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));
    }
}
