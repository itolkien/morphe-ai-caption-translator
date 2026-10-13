package app.yydarlinker.deepseekcaptions;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Experimental caption adapter. Keeps the original token ownership/times.
 * Each source cue window is independently translated, then checked against the
 * existing RebuildProtocol so Google never supplies invented timestamps.
 */
final class GoogleTranslationPlan {
    private GoogleTranslationPlan() {}

    static RebuildProtocol.Plan translate(RebuildSource source, RebuildPlanner.Block block,
            CaptionLanguageContext language,
            DeepSeekApiClient.RequestControl control) throws Exception {
        JSONArray events = new JSONArray();
        for (RebuildSource.TimedWindow window : source.windows()) {
            if (window.to < block.from || window.from > block.to) continue;
            int from = Math.max(block.from, window.from);
            int to = Math.min(block.to, window.to);
            if (control != null && control.isCancelled())
                throw new java.io.InterruptedIOException("cancelled");
            String original = source.text(from, to);
            String translated = GoogleTranslateClient.translate(
                    original, language.sourceCode, language.targetCode, control);
            events.put(new JSONObject()
                .put("from",from).put("to",to)
                .put("source",original).put("text",translated));
        }
        if (events.length() == 0) throw new RebuildProtocol.Invalid("empty_google_translation");
        String json = new JSONObject().put("block",block.id()).put("events",events).toString();
        return RebuildProtocol.parseBound(json, source, block, language);
    }
}
