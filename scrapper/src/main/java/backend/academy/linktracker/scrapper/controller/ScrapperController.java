package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.DeleteLinksByTagResponse;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.CachedScrapperService;
// import backend.academy.linktracker.scrapper.service.ScrapperService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@SuppressFBWarnings(
        value = "CRLF_INJECTION_LOGS",
        justification = "Все пользовательские данные проходят через cleanLogInput")
public class ScrapperController {

    // private final ScrapperService scrapperService;

    private final CachedScrapperService scrapperService;

    @PostMapping("/tg-chat/{id}")
    public ResponseEntity<Void> register(@PathVariable Long id) {

        log.info("api_register_chat chatId={}", cleanLogInput(String.valueOf(id)));

        scrapperService.registerChat(id);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tg-chat/{id}")
    public ResponseEntity<Void> unregister(@PathVariable Long id) {

        log.info("api_unregister_chat chatId={}", cleanLogInput(String.valueOf(id)));

        scrapperService.unregisterChat(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/links")
    public ResponseEntity<ListLinksResponse> list(@RequestHeader("Tg-Chat-Id") Long chatId) {

        log.info("api_list_links chatId={}", cleanLogInput(String.valueOf(chatId)));

        return ResponseEntity.ok(scrapperService.listLinks(chatId));
    }

    @PostMapping("/links")
    public ResponseEntity<LinkResponse> add(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody AddLinkRequest request) {

        log.info("api_add_link chatId={} url={}", cleanLogInput(String.valueOf(chatId)), cleanLogInput(request.link()));

        return ResponseEntity.ok(scrapperService.addLink(chatId, request));
    }

    @DeleteMapping("/links")
    public ResponseEntity<LinkResponse> remove(
            @RequestHeader("Tg-Chat-Id") Long chatId, @RequestBody RemoveLinkRequest request) {

        log.info(
                "api_remove_link chatId={} url={}",
                cleanLogInput(String.valueOf(chatId)),
                cleanLogInput(request.link()));

        return ResponseEntity.ok(scrapperService.removeLink(chatId, request));
    }

    @DeleteMapping("/links/by-tag/{tag}")
    public DeleteLinksByTagResponse removeLinksByTag(
            @RequestHeader("Tg-Chat-Id") Long chatId, @PathVariable String tag) {
        return scrapperService.removeLinksByTag(chatId, tag);
    }

    private String cleanLogInput(String value) {
        return value == null ? null : value.replaceAll("[\\n\\r]", "");
    }
}
