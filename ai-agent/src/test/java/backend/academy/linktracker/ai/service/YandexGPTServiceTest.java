package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YandexGPTServiceTest {

    private SummarizationGatewayService gatewayService;
    private YandexGPTService service;

    @BeforeEach
    void setUp() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setThreshold(10);

        gatewayService = mock(SummarizationGatewayService.class);

        service = new YandexGPTService(gatewayService, properties);
    }

    @Test
    void shouldSummarizeOnlyPreviewPart() {
        String text = """
                Обновление по ссылке: https://github.com/jamshut
                Тип: ISSUE
                Заголовок: Test issue
                Пользователь: ravshan
                Превью: nashaRassiaJamshutAndRavshan
                """;

        when(gatewayService.summarizationTextForYandexGPT("nashaRassiaJamshutAndRavshan"))
                .thenReturn("short summary");

        String result = service.summarize(text);

        assertEquals("""
                Обновление по ссылке: https://github.com/jamshut
                Тип: ISSUE
                Заголовок: Test issue
                Пользователь: ravshan
                Превью:
                short summary""", result);

        verify(gatewayService).summarizationTextForYandexGPT("nashaRassiaJamshutAndRavshan");
    }

    @Test
    void shouldNotCallYandexGptWhenPreviewIsShort() {
        String text = "Превью: short";

        String result = service.summarize(text);

        assertEquals(text, result);
        verifyNoInteractions(gatewayService);
    }

    @Test
    void shouldSummarizeWholeTextWhenPreviewMarkerIsMissing() {
        String text = "abcdefghijklmnopqrstuvwxyz";

        when(gatewayService.summarizationTextForYandexGPT(text)).thenReturn("short summary");

        String result = service.summarize(text);

        assertEquals("short summary", result);
        verify(gatewayService).summarizationTextForYandexGPT(text);
    }
}
