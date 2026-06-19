package backend.academy.linktracker.scrapper.util;

public final class PreviewUtils {

    private PreviewUtils() {}

    public static String shorten(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }

    public static String toPlainText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String noTags = text.replaceAll("(?is)<br\\s*/?>", " ")
                .replaceAll("(?is)</p>", " ")
                .replaceAll("(?is)<[^>]+>", " ");

        String decoded = noTags.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ");

        return decoded.replaceAll("\\s+", " ").trim();
    }

    public static String plainTextPreview(String text) {
        return shorten(toPlainText(text));
    }
}
