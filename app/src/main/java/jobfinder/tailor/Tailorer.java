package jobfinder.tailor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import jobfinder.profile.Profile;
import jobfinder.source.Job;

/**
 * Deterministic tailoring: orders the owner's real skills, proof points and experience bullets by word overlap with
 * the job text. It never rewrites or invents text. Highlights hold the top proof points with an overlap.
 */
public final class Tailorer {

    static final int MAX_HIGHLIGHTS = 5;
    private static final Pattern WORDS = Pattern.compile("[\\p{L}\\p{N}+#.]{2,}");

    private Tailorer() {}

    public static TailoredCv tailor(Job job, Profile profile) {
        Set<String> jobWords = words(job.title() + " " + (job.description() == null ? "" : job.description()));

        List<String> skills = profile.skills().stream()
                .sorted(Comparator.comparingInt((Profile.Skill s) -> overlap(jobWords, s.name())).reversed())
                .map(Profile.Skill::name).toList();

        List<Profile.ProofPoint> highlights = profile.proofPoints().stream()
                .filter(p -> overlap(jobWords, p.claim()) > 0)
                .sorted(Comparator.comparingInt((Profile.ProofPoint p) -> overlap(jobWords, p.claim())).reversed())
                .limit(MAX_HIGHLIGHTS).toList();

        List<Profile.Experience> experience = new ArrayList<>();
        for (Profile.Experience e : profile.experience()) {
            List<String> bullets = e.bullets().stream()
                    .sorted(Comparator.comparingInt((String b) -> overlap(jobWords, b)).reversed()).toList();
            experience.add(new Profile.Experience(e.company(), e.role(), e.from(), e.to(), bullets));
        }
        return new TailoredCv(job, profile.headline(), profile.summary(), skills, highlights, experience);
    }

    static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        var m = WORDS.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }

    static int overlap(Set<String> jobWords, String text) {
        int n = 0;
        for (String w : words(text)) if (jobWords.contains(w)) n++;
        return n;
    }
}
