package app.yydarlinker.deepseekcaptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;

/**
 * Experimental, keyless Google Translate adapter. This is NOT the supported Google Cloud
 * Translation API. The public web endpoint may rate-limit, change, or refuse requests.
 * Kept separate from RebuildApi until timing-preservation and UI routing are implemented.
 */
final class GoogleTranslateClient {
    private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single";
    private static final int MAX_CHARACTERS = 3500;
    private static final int MAX_RESPONSE_BYTES = 131072;

    private GoogleTranslateClient() {}

    static String translate(String text, String sourceLanguage, String targetLanguage,
                            DeepSeekApiClient.RequestControl control) throws Exception {
        if (text == null || text.isEmpty()) return "";
        if (text.length() > MAX_CHARACTERS) throw new IllegalArgumentException("translation_text_too_large");
        String sl = normalizedLanguage(sourceLanguage, "auto");
        String tl = normalizedLanguage(targetLanguage, null);
        if (tl == null) throw new IllegalArgumentException("missing_target_language");
        String query = "client=gtx&dt=t&sl=" + URLEncoder.encode(sl, "UTF-8")
                + "&tl=" + URLEncoder.encode(tl, "UTF-8")
                + "&q=" + URLEncoder.encode(text, "UTF-8");
        HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT + "?" + query).openConnection();
        try {
            if (control != null) {
                if (control.isCancelled()) throw new java.io.InterruptedIOException("cancelled");
                control.onConnection(c);
            }
            c.setRequestMethod("GET");
            c.setInstanceFollowRedirects(false);
            c.setUseCaches(false);
            c.setConnectTimeout(4000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/json");
            int status = c.getResponseCode();
            if (status != 200) throw new java.io.IOException("google_translate_http_" + status);
            byte[] raw;
            try (InputStream in = c.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (control != null && control.isCancelled()) throw new java.io.InterruptedIOException("cancelled");
                    if (out.size() + n > MAX_RESPONSE_BYTES) throw new java.io.IOException("google_translate_response_too_large");
                    out.write(buf, 0, n);
                }
                raw = out.toByteArray();
            }
            return parse(new String(raw, StandardCharsets.UTF_8));
        } finally {
            c.disconnect();
        }
    }

    static String parse(String body) throws Exception {
        JSONArray root = new JSONArray(body);
        JSONArray parts = root.getJSONArray(0);
        StringBuilder translated = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONArray segment = parts.optJSONArray(i);
            if (segment != null && !segment.isNull(0)) translated.append(segment.getString(0));
        }
        if (translated.length() == 0) throw new java.io.IOException("google_translate_empty_response");
        return translated.toString();
    }

    static String normalizedLanguage(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String code = value.trim().replace('_', '-');
        if ("UNKNOWN".equalsIgnoreCase(code) || "und".equalsIgnoreCase(code)) return fallback;
        if ("zh-Hans".equalsIgnoreCase(code)) return "zh-CN";
        if ("zh-Hant".equalsIgnoreCase(code)) return "zh-TW";
        if (!code.matches("[a-zA-Z]{2,8}(?:-[a-zA-Z0-9]{2,8})?"))
            throw new IllegalArgumentException("invalid_language_code");
        return code;
    }
}
