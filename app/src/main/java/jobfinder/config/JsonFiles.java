package jobfinder.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import jobfinder.profile.Profile;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public final class JsonFiles {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private JsonFiles() {}

    public static AppConfig loadConfig(Path path) throws IOException {
        AppConfig config = MAPPER.readValue(Files.readString(path), AppConfig.class);
        if (config.maxAgeDays() <= 0) throw new IllegalStateException("max_age_days must be > 0");
        if (config.sources() == null || config.sources().isEmpty()) throw new IllegalStateException("sources is empty");
        return config;
    }

    public static Profile loadProfile(Path path) throws IOException {
        Profile profile = MAPPER.readValue(Files.readString(path), Profile.class);
        Set<String> ids = new java.util.HashSet<>();
        for (Profile.ProofPoint p : profile.proofPoints()) {
            boolean ok = "cv".equals(p.source()) || p.source().startsWith("owner-stated:");
            if (!ok) throw new IllegalStateException("Proof point " + p.id() + " has ungrounded source: " + p.source());
            ids.add(p.id());
        }
        for (Profile.Skill s : profile.skills()) {
            for (String id : s.proofPointIds()) {
                if (!ids.contains(id)) throw new IllegalStateException("Skill " + s.name() + " cites unknown proof point " + id);
            }
        }
        if (profile.education() != null) for (Profile.Education e : profile.education()) requireSource("Education " + e.institution(), e.source());
        if (profile.languages() != null) for (Profile.Language l : profile.languages()) requireSource("Language " + l.name(), l.source());
        return profile;
    }

    private static void requireSource(String what, String source) {
        if (source == null || !(source.equals("cv") || source.startsWith("owner-stated:")))
            throw new IllegalStateException(what + " has ungrounded source: " + source);
    }
}
