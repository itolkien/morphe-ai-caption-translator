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
        java.util.List<RebuildProtocol.Event> timed = new java.util.ArrayList<>();
        for (RebuildSource.TimedWindow window : source.windows()) {
            if (window.to < block.from || window.from > block.to) continue;
            int from = Math.max(block.from, window.from);
            int to = Math.min(block.to, window.to);
            if (control != null && control.isCancelled())
                throw new java.io.InterruptedIOException("cancelled");
            String original = source.text(from, to);
            String translated = GoogleTranslateClient.translate(
                    original, language.sourceCode, language.targetCode, control);
            RebuildSource.Word first = source.words.get(from);
            RebuildSource.Word last = source.words.get(to);
            timed.add(new RebuildProtocol.Event(from,to,first.start,last.end,translated));
            events.put(new JSONObject()
                .put("from",from).put("to",to)
                .put("source",original).put("text",translated));
        }
        if (events.length() == 0) throw new RebuildProtocol.Invalid("empty_google_translation");
        String json = new JSONObject().put("block",block.id()).put("events",events).toString();
        // Google provides plain text, not an AI-generated source ownership protocol.
        // Avoid model-only fidelity/segmentation validators while keeping source-owned times.
        return new RebuildProtocol.Plan(timed, json);
    }
}
