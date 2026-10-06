package jobfinder.track;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import jobfinder.score.ScoredJob;
import jobfinder.source.Job;

/** Renders the daily shortlist as Markdown. Read-only output; never acts on a posting. */
public final class ShortlistReport {

    private ShortlistReport() {}

    public static String render(List<ScoredJob> jobs, LocalDate date) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Job shortlist ").append(date).append("\n\n");
        sb.append("Jobs: ").append(jobs.size()).append("\n\n");
        int i = 1;
        for (ScoredJob s : jobs) {
            Job j = s.job();
            sb.append("## ").append(i++).append(". ").append(esc(j.title())).append(" at ").append(esc(j.company())).append("\n\n");
            sb.append("- Score: ").append(String.format(Locale.ROOT, "%.1f", s.score())).append("\n");
            sb.append("- Reason: ").append(esc(s.reason())).append("\n");
            sb.append("- Location: ").append(esc(j.location())).append("\n");
            sb.append("- Posted: ").append(j.datePosted() == null ? "unknown" : j.datePosted().toString()).append("\n");
            sb.append("- URL: ").append(url(j.url())).append("\n\n");
        }
        return sb.toString();
    }

    public static void write(Path path, List<ScoredJob> jobs, LocalDate date) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(path, render(jobs, date));
    }

    private static String url(String u) {
        if (u == null || u.isBlank()) return "n/a";
        return "<" + u.strip().replace(" ", "%20").replace("<", "%3C").replace(">", "%3E").replaceAll("[\\r\\n]", "") + ">";
    }

    static String esc(String s) {
        if (s == null || s.isBlank()) return "n/a";
        String t = s.strip().replaceAll("\\s*[\\r\\n]+\\s*", " ");
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray()) {
            if ("\\`*_{}[]()#+-.!|<>~&".indexOf(c) >= 0) sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }
}
