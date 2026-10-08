package jobfinder.tailor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jobfinder.profile.Profile;

/** Renders a TailoredCv to PDF: HTML template (resources/cv-template.html) turned into PDF by openhtmltopdf. */
public final class CvPdf {

    private CvPdf() {}

    public static String html(TailoredCv cv, Profile profile) throws IOException {
        String template;
        try (var in = CvPdf.class.getResourceAsStream("/cv-template.html")) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return template
                .replace("{{name}}", esc(profile.name()))
                .replace("{{headline}}", esc(cv.headline()))
                .replace("{{contact}}", esc(profile.contact() == null ? "" : String.join(" | ", profile.contact())))
                .replace("{{summary}}", esc(cv.summary()))
                .replace("{{highlights}}", highlights(cv.highlights()))
                .replace("{{skills}}", esc(String.join(", ", cv.skills())))
                .replace("{{experience}}", experience(cv.experience()))
                .replace("{{education}}", education(profile.education()))
                .replace("{{languages}}", languages(profile.languages()));
    }

    /** Education and languages come straight from the profile, so they are grounded by construction. */
    private static String education(List<Profile.Education> es) {
        if (es == null || es.isEmpty()) return "";
        var sb = new StringBuilder("<h2>Education</h2>");
        for (var e : es) {
            sb.append("<div class=\"job\"><table class=\"jobhead\"><tr>")
              .append("<td class=\"role\">").append(esc(e.title())).append("</td>");
            if (e.from() != null) sb.append("<td class=\"dates\">").append(esc(e.from())).append(" – ").append(e.to() == null ? "present" : esc(e.to())).append("</td>");
            sb.append("</tr></table><p class=\"company\">").append(esc(e.institution())).append("</p></div>");
        }
        return sb.toString();
    }

    private static String languages(List<Profile.Language> ls) {
        if (ls == null || ls.isEmpty()) return "";
        var parts = new java.util.ArrayList<String>();
        for (var l : ls) parts.add(esc(l.name()) + " (" + esc(l.level()) + ")");
        return "<h2>Languages</h2><p class=\"skills\">" + String.join(" &#183; ", parts) + "</p>";
    }

    /** Verifies grounding first, then writes the PDF. Throws if any text is not in the profile. */
    public static Path write(TailoredCv cv, Profile profile, Path file) throws IOException {
        Grounding.verify(cv, profile);
        Files.createDirectories(file.toAbsolutePath().getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            var b = new PdfRendererBuilder();
            b.useFastMode();
            b.withHtmlContent(html(cv, profile), null);
            b.toStream(out);
            b.run();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("PDF rendering failed: " + e.getMessage(), e);
        }
        return file;
    }

    private static String highlights(List<Profile.ProofPoint> hs) {
        if (hs.isEmpty()) return "";
        var sb = new StringBuilder("<h2>Relevant highlights</h2><ul>");
        for (var h : hs) sb.append("<li>").append(esc(h.claim())).append("</li>");
        return sb.append("</ul>").toString();
    }

    // CHANGED: two-line job head. Line 1 = role (bold, left) | dates (right) in a two-cell table.
    // Line 2 = company (italic). Text values are unchanged; only markup and separators differ.
    private static String experience(List<Profile.Experience> es) {
        var sb = new StringBuilder();
        for (var e : es) {
            sb.append("<div class=\"job\"><table class=\"jobhead\"><tr>")
              .append("<td class=\"role\">").append(esc(e.role())).append("</td>")
              .append("<td class=\"dates\">").append(esc(e.from())).append(" \u2013 ")
              .append(e.to() == null ? "present" : esc(e.to())).append("</td>")
              .append("</tr></table>")
              .append("<p class=\"company\">").append(esc(e.company())).append("</p>");
            if (!e.bullets().isEmpty()) {
                sb.append("<ul>");
                for (String b : e.bullets()) sb.append("<li>").append(esc(b)).append("</li>");
                sb.append("</ul>");
            }
            sb.append("</div>");
        }
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
