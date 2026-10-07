package jobfinder.profile;

import java.util.List;

/** Candidate profile, loaded from private/profile.json (gitignored). Every proof point names its source. */
public record Profile(
        String name,
        String headline,
        String summary,
        List<Skill> skills,
        List<Experience> experience,
        List<ProofPoint> proofPoints,
        List<String> contact,
        List<String> preferences) {

    /** Convenience for callers without stated job preferences. */
    public Profile(String name, String headline, String summary, List<Skill> skills, List<Experience> experience,
                   List<ProofPoint> proofPoints, List<String> contact) {
        this(name, headline, summary, skills, experience, proofPoints, contact, List.of());
    }

    public record Skill(String name, String level, Integer years, List<String> proofPointIds) {}

    public record Experience(String company, String role, String from, String to, List<String> bullets) {}

    /** source is "cv" or "owner-stated:YYYY-MM-DD". Anything else is rejected by the loader. */
    public record ProofPoint(String id, String claim, String source, boolean confirmed) {}
}
