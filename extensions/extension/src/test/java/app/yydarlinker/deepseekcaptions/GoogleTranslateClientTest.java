package app.yydarlinker.deepseekcaptions;

import static org.junit.Assert.*;
import org.junit.Test;

/** Unit coverage for the standalone experimental Google Translate response adapter. */
public class GoogleTranslateClientTest {
    @Test public void joinsResponseSegmentsInOrder() throws Exception {
        assertEquals("Merhaba dünya", GoogleTranslateClient.parse(
            "[[[\"Merhaba \",\"Hello \",null,null,1],[\"dünya\",\"world\",null,null,1]],null,\"en\"]"));
    }
    @Test public void rejectsMalformedPayload() {
        try {
            GoogleTranslateClient.parse("{}");
            fail("expected invalid payload");
        } catch (Exception expected) { /* malformed payload */ }
    }
    @Test public void validatesLanguageCode() {
        assertEquals("tr",GoogleTranslateClient.normalizedLanguage("tr",null));
        assertEquals("auto",GoogleTranslateClient.normalizedLanguage("", "auto"));
        try {
            GoogleTranslateClient.normalizedLanguage("tr&x=evil",null);
            fail("expected invalid language");
        } catch (IllegalArgumentException expected) { /* invalid language */ }
    }
}
