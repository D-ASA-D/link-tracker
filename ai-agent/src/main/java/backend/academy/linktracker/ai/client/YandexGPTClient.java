package backend.academy.linktracker.ai.client;

import backend.academy.linktracker.ai.dto.yandex.YandexGptRequest;
import backend.academy.linktracker.ai.dto.yandex.YandexGptResponse;
import backend.academy.linktracker.ai.properties.YandexGPTProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class YandexGPTClient {

    private static final String AUTHORIZATION_PREFIX = "Api-Key ";

    private final RestClient restClient;
    private final YandexGPTProperties properties;

    public String summarize(String previewText) {
        YandexGptRequest request = buildRequest(previewText);

        log.info("yandex_gpt_request_started previewLength={}", previewText == null ? 0 : previewText.length());

        YandexGptResponse response = restClient
                .post()
                .uri(properties.getApiProperties().getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        AUTHORIZATION_PREFIX + properties.getApiProperties().getKey())
                .body(request)
                .retrieve()
                .body(YandexGptResponse.class);

        String result = extractText(response);

        log.info("yandex_gpt_request_finished resultLength={}", result.length());

        return result;
    }

    private YandexGptRequest buildRequest(String previewText) {
        return new YandexGptRequest(
                modelUri(),
                new YandexGptRequest.CompletionOptions(false, properties.getTemperature(), properties.getMaxTokens()),
                List.of(
                        new YandexGptRequest.Message(
                                "system", properties.getPrompt().getSystem()),
                        new YandexGptRequest.Message("user", previewText)));
    }

    private String modelUri() {
        return "gpt://" + properties.getFolderSettings().getId() + "/" + properties.getModel();
    }

    private String extractText(YandexGptResponse response) {
        if (response == null
                || response.result() == null
                || response.result().alternatives() == null
                || response.result().alternatives().isEmpty()
                || response.result().alternatives().getFirst().message() == null
                || response.result().alternatives().getFirst().message().text() == null) {
            throw new IllegalStateException("Empty response from YandexGPT");
        }

        return response.result().alternatives().getFirst().message().text();
    }
}
