package jobfinder.tailor;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jobfinder.profile.Profile;
import jobfinder.source.Job;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class TailorTest {

    static final Profile PROFILE = new Profile("Ana Perez", "Java Developer", "Backend dev & more <3",
            List.of(new Profile.Skill("Java", null, null, List.of("pp1")), new Profile.Skill("React", null, null, List.of("pp2")),
                    new Profile.Skill("OAuth and token standards", null, null, List.of("pp3"))),
            List.of(new Profile.Experience("Acme", "Dev", "2022-01", null,
                    List.of("Built React dashboards for support", "Designed OAuth token signing lambda"))),
            List.of(new Profile.ProofPoint("pp1", "Built Java services at scale", "cv", true),
                    new Profile.ProofPoint("pp2", "Built React tools", "cv", true),
                    new Profile.ProofPoint("pp3", "Implemented an OAuth authorization server lambda", "owner-stated:2026-10-06", true)),
            List.of("ana@example.test", "linkedin.com/in/ana"));

    static Job job(String desc) {
        return new Job("linkedin", "1", "u", "Java Backend Engineer", "Co", "AR", null, null, null, null, desc);
    }

    @Test
    void ordersSkillsBulletsAndHighlightsByJobOverlap() {
        var cv = Tailorer.tailor(job("We need OAuth token expertise with Java"), PROFILE);
        assertEquals(2, java.util.Set.of("Java", "OAuth and token standards").stream().filter(s -> cv.skills().subList(0, 2).contains(s)).count()); // both matching skills first
        assertEquals("Designed OAuth token signing lambda", cv.experience().get(0).bullets().get(0));
        assertTrue(cv.highlights().stream().noneMatch(h -> h.id().equals("pp2"))); // React: no overlap, not highlighted
        assertTrue(cv.highlights().stream().anyMatch(h -> h.id().equals("pp3")));
        Grounding.verify(cv, PROFILE);
    }

    @Test
    void groundingRejectsAnyInventedText() {
        var cv = Tailorer.tailor(job("Java"), PROFILE);
        var badSkills = new TailoredCv(cv.job(), cv.headline(), cv.summary(), List.of("Java", "C expert"), cv.highlights(), cv.experience());
        assertThrows(IllegalStateException.class, () -> Grounding.verify(badSkills, PROFILE));

        var badBullet = new Profile.Experience("Acme", "Dev", "2022-01", null, List.of("Led a team of 50"));
        var cv2 = new TailoredCv(cv.job(), cv.headline(), cv.summary(), cv.skills(), cv.highlights(), List.of(badBullet));
        assertThrows(IllegalStateException.class, () -> Grounding.verify(cv2, PROFILE));

        var badSummary = new TailoredCv(cv.job(), cv.headline(), "Expert in everything", cv.skills(), cv.highlights(), cv.experience());
        assertThrows(IllegalStateException.class, () -> Grounding.verify(badSummary, PROFILE));
    }

    /** Renders fake data to target/cv-sample.pdf (gitignored) so the layout can be checked by eye. */
    @Test
    void rendersSamplePdfForVisualCheck() throws Exception {
        var bullets = List.of("Built Java services at scale", "Built React tools", "Implemented an OAuth authorization server lambda");
        var jobs = new java.util.ArrayList<Profile.Experience>();
        for (int i = 0; i < 5; i++) jobs.add(new Profile.Experience("Company " + i, "Senior Backend Engineer " + i, "20" + (10 + i) + "-01", i == 0 ? null : "20" + (11 + i) + "-06", bullets));
        var profile = new Profile("Ana Perez", "Java Developer", "Backend dev with fake data", PROFILE.skills(), jobs, PROFILE.proofPoints(), PROFILE.contact());
        var cv = Tailorer.tailor(job("Java OAuth React"), profile);
        Path pdf = CvPdf.write(cv, profile, Path.of("target", "cv-sample.pdf"));
        assertTrue(Files.size(pdf) > 1000);
    }

    @Test
    void educationAndLanguagesAreRenderedFromProfile() throws Exception {
        var profile = new Profile(PROFILE.name(), PROFILE.headline(), PROFILE.summary(), PROFILE.skills(), PROFILE.experience(),
                PROFILE.proofPoints(), PROFILE.contact(), List.of(),
                List.of(new Profile.Education("Test University", "Systems Analyst", "2018", "2021", "owner-stated:2026-10-08")),
                List.of(new Profile.Language("English", "Professional working", "cv")));
        Path pdf = CvPdf.write(Tailorer.tailor(job("Java"), profile), profile, Files.createTempDirectory("cv").resolve("edu.pdf"));
        try (var doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Systems Analyst") && text.contains("Test University") && text.contains("2018"));
            assertTrue(text.contains("English (Professional working)"));
        }
    }

    @Test
    void writesRealPdfWithEscapedTextAndRefusesUngroundedCv() throws Exception {
        var cv = Tailorer.tailor(job("Java OAuth"), PROFILE);
        Path pdf = CvPdf.write(cv, PROFILE, Files.createTempDirectory("cv").resolve("out.pdf"));
        assertTrue(Files.size(pdf) > 1000);
        assertEquals("%PDF", new String(Files.readAllBytes(pdf), 0, 4));
        try (var doc = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Ana Perez"));
            assertTrue(text.contains("Designed OAuth token signing lambda"));
            assertTrue(text.contains("Backend dev & more <3")); // special characters survive escaping
        }
        var bad = new TailoredCv(cv.job(), cv.headline(), "invented", cv.skills(), cv.highlights(), cv.experience());
        assertThrows(IllegalStateException.class, () -> CvPdf.write(bad, PROFILE, pdf.resolveSibling("bad.pdf")));
    }
}
