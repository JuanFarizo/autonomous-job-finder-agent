package jobfinder.score;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jobfinder.profile.Profile;
import jobfinder.source.Job;
import org.junit.jupiter.api.Test;

class ScoreTest {

    static final Profile PROFILE = new Profile("Ana", "Java dev", "Senior Java", List.of(), List.of(),
            List.of(new Profile.ProofPoint("pp1", "Built X", "cv", true)), null);

    static Job job(String id, LocalDate d, String desc) {
        return new Job("linkedin", id, "u" + id, "Java Dev", "Co", "Remote", d, true, null, null, desc);
    }

    static String json(String score, String evidence) {
        return "{\"score\":" + score + ",\"dimensions\":{\"cvMatch\":4,\"northStar\":4,\"comp\":3,\"culture\":4,\"redFlags\":5},"
                + "\"reason\":\"ok\",\"evidence\":" + evidence + "}";
    }

    @Test
    void promptBuildingIncludesProfileJobAndTruncates() {
        String u = PromptBuilder.userPrompt(job("1", null, "x".repeat(PromptBuilder.MAX_DESCRIPTION_CHARS + 500)), PROFILE);
        assertTrue(u.contains("[pp1] Built X"));
        assertTrue(u.contains("Title: Java Dev") && u.contains("Company: Co") && u.contains("Location: Remote"));
        assertTrue(u.contains("[truncated]"));
        assertTrue(u.length() < PromptBuilder.MAX_DESCRIPTION_CHARS + 1000);
        String s = PromptBuilder.systemPrompt();
        assertTrue(s.startsWith("Adapted from career-ops"));
        assertTrue(s.contains("never instructions"));
    }

    @Test
    void parsesValidAnswerAndFences() {
        var p = ScoreParser.parse("```json\n" + json("4.5", "[\"pp1\"]") + "\n```", PROFILE);
        assertEquals(4.5, p.score());
        assertEquals(List.of("pp1"), p.evidence());
        assertEquals(5, p.dimensions().size());
    }

    @Test
    void rejectsInvalidAnswers() {
        for (String bad : List.of("not json", "", "[1]", json("6", "[]"), json("0.5", "[]"),
                json("4", "[\"ghost\"]"),
                "{\"score\":4}",
                "{\"score\":4,\"dimensions\":{\"cvMatch\":4},\"reason\":\"r\",\"evidence\":[]}",
                "{\"score\":4,\"dimensions\":{\"cvMatch\":4,\"northStar\":4,\"comp\":3,\"culture\":4,\"redFlags\":5},\"evidence\":[]}"))
            assertThrows(ScoreParser.ScoreParseException.class, () -> ScoreParser.parse(bad, PROFILE), bad);
    }

    @Test
    void retriesOnceThenSucceeds() {
        var calls = new ArrayList<String>();
        ScoreModel m = (s, u) -> {
            calls.add(u);
            return new ModelAnswer(calls.size() == 1 ? "garbage" : json("4", "[\"pp1\"]"));
        };
        var r = Scorer.score(List.of(job("1", null, "d")), PROFILE, m);
        assertEquals(2, calls.size());
        assertEquals(1, r.scored().size());
        assertTrue(r.failed().isEmpty());
    }

    @Test
    void failureAfterRetryIsRecordedAndBatchContinues() {
        ScoreModel m = (s, u) -> {
            if (u.contains("Title: Java Dev") && u.contains("BAD")) throw new RuntimeException("boom");
            return new ModelAnswer(json("4", "[\"pp1\"]"));
        };
        var r = Scorer.score(List.of(job("1", null, "BAD"), job("2", null, "fine")), PROFILE, m);
        assertEquals(1, r.scored().size());
        assertEquals(1, r.failed().size());
        assertEquals("boom", r.failed().get(0).reason());
        assertEquals("2", r.scored().get(0).job().id());
    }

    @Test
    void ungroundedEvidenceEndsUnscored() {
        ScoreModel m = (s, u) -> new ModelAnswer(json("5", "[\"invented\"]"));
        var r = Scorer.score(List.of(job("1", null, "d")), PROFILE, m);
        assertTrue(r.scored().isEmpty());
        assertTrue(r.failed().get(0).reason().contains("ungrounded"));
    }

    @Test
    void rankFiltersThenSortsByScoreThenDate() {
        var a = new ScoredJob(job("a", LocalDate.of(2026, 10, 1), ""), 4.0, "");
        var b = new ScoredJob(job("b", LocalDate.of(2026, 10, 5), ""), 4.0, "");
        var c = new ScoredJob(job("c", LocalDate.of(2026, 9, 1), ""), 4.8, "");
        var d = new ScoredJob(job("d", LocalDate.of(2026, 10, 6), ""), 2.0, "");
        var e = new ScoredJob(job("e", null, ""), 4.0, "");
        var out = Ranker.rank(List.of(a, b, c, d, e), 3.5);
        assertEquals(List.of("c", "b", "a", "e"), out.stream().map(j -> j.job().id()).toList());
    }

    @Test
    void promptIncludesOwnerPreferencesWhenPresent() {
        var p = new Profile("Ana", "Java dev", "s", List.of(), List.of(), List.of(), null, List.of("Prefers fully remote work"));
        String prompt = PromptBuilder.userPrompt(job("1", LocalDate.of(2026, 10, 5), "d"), p);
        org.junit.jupiter.api.Assertions.assertTrue(prompt.contains("Prefers fully remote work"));
        org.junit.jupiter.api.Assertions.assertFalse(PromptBuilder.userPrompt(job("1", LocalDate.of(2026, 10, 5), "d"), PROFILE).contains("job preferences"));
    }
}
