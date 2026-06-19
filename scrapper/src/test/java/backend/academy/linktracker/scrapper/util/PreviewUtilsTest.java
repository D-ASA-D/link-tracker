package backend.academy.linktracker.scrapper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PreviewUtilsTest {

    @Test
    void shorten_shouldReturnEmptyString_whenTextIsNull() {
        assertEquals("", PreviewUtils.shorten(null));
    }

    @Test
    void shorten_shouldReturnEmptyString_whenTextIsBlank() {
        assertEquals("", PreviewUtils.shorten("   \n\t   "));
    }

    @Test
    void shorten_shouldReturnSameText_whenLengthIsLessThan200() {
        String text = "Hello world";
        assertEquals(text, PreviewUtils.shorten(text));
    }

    @Test
    void shorten_shouldNormalizeSpaces() {
        String text = "Hello    world \n this   is\t test";
        assertEquals("Hello world this is test", PreviewUtils.shorten(text));
    }

    @Test
    void shorten_shouldNotTrimText_whenTextIsTooLong() {
        String text = "a".repeat(250);

        String result = PreviewUtils.shorten(text);

        assertEquals(250, result.length());
        assertEquals("a".repeat(250), result);
    }

    @Test
    void toPlainText_shouldRemoveHtmlTags() {
        String html = "<p>Hello <a href=\"https://example.com\">world</a></p>";
        assertEquals("Hello world", PreviewUtils.toPlainText(html));
    }

    @Test
    void plainTextPreview_shouldRemoveHtmlAndNotTrimText() {
        String html = "<p>" + "a".repeat(250) + "</p>";
        String result = PreviewUtils.plainTextPreview(html);

        assertEquals(250, result.length());
        assertEquals("a".repeat(250), result);
    }
}
