package jobfinder.score;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jobfinder.profile.Profile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Parses and validates the model JSON answer, including the grounding check on evidence ids. */
public final class ScoreParser {

    public static final List<String> DIMENSIONS = List.of("cvMatch", "northStar", "comp", "culture", "redFlags");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ScoreParser() {}

    /** @throws ScoreParseException on any invalid answer */
    public static ParsedScore parse(String raw, Profile profile) {
        if (raw == null || raw.isBlank()) throw new ScoreParseException("empty answer");
        JsonNode root;
        try {
            root = MAPPER.readTree(stripFences(raw));
        } catch (Exception e) {
            throw new ScoreParseException("invalid JSON: " + e.getMessage());
        }
        if (root == null || !root.isObject()) throw new ScoreParseException("answer is not a JSON object");

        double score = number(root.get("score"), "score");

        JsonNode dims = root.get("dimensions");
        if (dims == null || !dims.isObject()) throw new ScoreParseException("missing field: dimensions");
        Map<String, Double> dimensions = new LinkedHashMap<>();
        for (String d : DIMENSIONS) dimensions.put(d, number(dims.get(d), "dimensions." + d));

        JsonNode reasonNode = root.get("reason");
        if (reasonNode == null || !reasonNode.isString() || reasonNode.asString().isBlank())
            throw new ScoreParseException("missing field: reason");

        JsonNode ev = root.get("evidence");
        if (ev == null || !ev.isArray()) throw new ScoreParseException("missing field: evidence");
        Set<String> known = profile.proofPoints().stream().map(Profile.ProofPoint::id).collect(Collectors.toSet());
        List<String> evidence = new ArrayList<>();
        for (JsonNode e : ev) {
            if (!e.isString()) throw new ScoreParseException("evidence entries must be strings");
            String id = e.asString();
            if (!known.contains(id)) throw new ScoreParseException("ungrounded evidence id: " + id);
            evidence.add(id);
        }
        return new ParsedScore(score, dimensions, reasonNode.asString(), evidence);
    }

    private static double number(JsonNode n, String field) {
        if (n == null || n.isNull()) throw new ScoreParseException("missing field: " + field);
        if (!n.isNumber()) throw new ScoreParseException("not a number: " + field);
        double v = n.asDouble();
        if (v < 1 || v > 5) throw new ScoreParseException("out of range 1..5: " + field + "=" + v);
        return v;
    }

    /** Models often wrap JSON in markdown fences despite instructions; tolerate that only. */
    private static String stripFences(String s) {
        String t = s.strip();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) t = t.substring(nl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.strip();
    }

    public static final class ScoreParseException extends RuntimeException {
        public ScoreParseException(String message) { super(message); }
    }
}
