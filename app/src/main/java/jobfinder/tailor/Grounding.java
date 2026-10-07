package jobfinder.tailor;

import java.util.HashSet;
import java.util.Set;

import jobfinder.profile.Profile;

/** Grounding rule (C8): every piece of CV text must exist in the owner's profile. */
public final class Grounding {

    private Grounding() {}

    public static void verify(TailoredCv cv, Profile profile) {
        if (!equalsOrNull(cv.summary(), profile.summary())) throw new IllegalStateException("Ungrounded summary");
        if (!equalsOrNull(cv.headline(), profile.headline())) throw new IllegalStateException("Ungrounded headline");

        Set<String> skills = new HashSet<>();
        profile.skills().forEach(s -> skills.add(s.name()));
        for (String s : cv.skills()) if (!skills.contains(s)) throw new IllegalStateException("Ungrounded skill: " + s);

        for (Profile.ProofPoint p : cv.highlights()) {
            boolean ok = profile.proofPoints().stream().anyMatch(q -> q.id().equals(p.id()) && q.claim().equals(p.claim()));
            if (!ok) throw new IllegalStateException("Ungrounded highlight: " + p.id());
        }

        for (Profile.Experience e : cv.experience()) {
            Profile.Experience real = profile.experience().stream()
                    .filter(x -> x.company().equals(e.company()) && x.role().equals(e.role())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Ungrounded experience: " + e.company()));
            for (String b : e.bullets()) {
                if (!real.bullets().contains(b)) throw new IllegalStateException("Ungrounded bullet: " + b);
            }
        }
    }

    private static boolean equalsOrNull(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
