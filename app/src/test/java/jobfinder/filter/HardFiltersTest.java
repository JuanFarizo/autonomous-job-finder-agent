package jobfinder.filter;

import jobfinder.config.AppConfig;
import jobfinder.source.Job;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HardFiltersTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    static int n = 0;

    static AppConfig cfg(List<String> exclude, Boolean onsite) {
        return new AppConfig(7, List.of("linkedin"), List.of(), List.of("semi-senior", "senior"), List.of("es", "en"),
                new AppConfig.LocationRules("AR", true, true, true, onsite), exclude, null, null);
    }

    static AppConfig cfg() { return cfg(List.of(), true); }

    static Job job(String title, String location, Boolean remote, LocalDate date, String desc) {
        n++;
        return new Job("linkedin", "id" + n, "https://x/" + n, title, "Acme" + n, location, date, remote, null, null, desc);
    }

    static Job ok(String title, String location, Boolean remote) {
        return job(title, location, remote, TODAY.minusDays(1), "Java backend role.");
    }

    static FilterResult run(AppConfig c, Job... jobs) { return HardFilters.apply(List.of(jobs), c, TODAY); }

    static String reason(FilterResult r) { return r.rejected().get(0).reason(); }

    @Test void dedupBySourceAndId() {
        Job a = ok("Java Dev", "Buenos Aires, Argentina", false);
        Job b = new Job(a.source(), a.id(), "https://other", "Other", "Other", "X", a.datePosted(), true, null, null, "d");
        FilterResult r = run(cfg(), a, b);
        assertEquals(List.of(a), r.kept());
        assertEquals("duplicate", reason(r));
    }

    @Test void dedupByUrl() {
        Job a = ok("Java Dev", "Buenos Aires, Argentina", false);
        Job b = new Job("linkedin", "zzz", a.url(), "Other", "Other", "Cordoba, Argentina", a.datePosted(), false, null, null, "d");
        assertEquals(1, run(cfg(), a, b).rejected().size());
    }

    @Test void dedupByNormalizedTitleCompanyLocation() {
        Job a = new Job("linkedin", "1", "u1", "Java Developer", "Acme", "Córdoba, Argentina", TODAY, false, null, null, "d");
        Job b = new Job("linkedin", "2", "u2", " java  DEVELOPER ", "ACME", "Cordoba, Argentina", TODAY, false, null, null, "d");
        FilterResult r = run(cfg(), a, b);
        assertEquals(List.of(a), r.kept());
        assertEquals("duplicate", reason(r));
    }

    @Test void staleRejectedAndBoundaryKept() {
        Job stale = job("Java Dev", "Argentina", false, TODAY.minusDays(8), "d");
        Job edge = job("Java Dev B", "Argentina", false, TODAY.minusDays(7), "d");
        FilterResult r = run(cfg(), stale, edge);
        assertEquals(List.of(edge), r.kept());
        assertEquals("stale", reason(r));
    }

    @Test void nullDateKeptWithWarning() {
        Job j = job("Java Dev", "Argentina", false, null, "d");
        FilterResult r = run(cfg(), j);
        assertEquals(1, r.kept().size());
        assertTrue(r.warnings().get(0).startsWith("date-unknown"));
    }

    @Test void argentinaOnsiteAcceptedWhenConfigured() {
        assertEquals(1, run(cfg(), ok("Java Dev", "Buenos Aires, Argentina", false)).kept().size());
    }

    @Test void argentinaOnsiteRejectedWhenNotAccepted() {
        assertEquals("onsite-not-accepted", reason(run(cfg(List.of(), false), ok("Java Dev", "Buenos Aires, Argentina", false))));
    }

    @Test void argentinaOnsiteUndecidedWarns() {
        FilterResult r = run(cfg(List.of(), null), ok("Java Dev", "Rosario, Argentina", false));
        assertEquals(1, r.kept().size());
        assertTrue(r.warnings().get(0).startsWith("onsite-undecided"));
    }

    @Test void remoteWorldwideKept() {
        FilterResult r = run(cfg(), job("Java Dev", "", true, TODAY, "Work from anywhere."));
        assertEquals(1, r.kept().size());
    }

    @Test void remoteUsOnlyRejectedEnglish() {
        FilterResult r = run(cfg(), job("Java Dev", "Remote", true, TODAY, "Remote, US only. Great team."));
        assertTrue(reason(r).startsWith("remote-restricted"));
    }

    @Test void mustBeLocatedInRejected() {
        FilterResult r = run(cfg(), job("Java Dev", "Remote", true, TODAY, "You must be located in the United States."));
        assertTrue(reason(r).startsWith("remote-restricted"));
    }

    @Test void soloEeuuRejectedSpanish() {
        FilterResult r = run(cfg(), job("Java Dev", "Remoto", true, TODAY, "Posición remota, solo EEUU."));
        assertTrue(reason(r).startsWith("remote-restricted"));
    }

    @Test void europeOnlyInTitleRejected() {
        FilterResult r = run(cfg(), job("Java Dev (Europe only)", "Remote", true, TODAY, "d"));
        assertTrue(reason(r).startsWith("remote-restricted"));
    }

    @Test void restrictionPlusLatamIsAmbiguousKept() {
        FilterResult r = run(cfg(), job("Java Dev", "Remote", true, TODAY, "US only for some roles; LATAM welcome."));
        assertEquals(1, r.kept().size());
        assertTrue(r.warnings().get(0).startsWith("location-ambiguous"));
    }

    @Test void remoteOtherCountryNoTextKeptWithWarning() {
        FilterResult r = run(cfg(), job("Java Dev", "United States", true, TODAY, "Nice job."));
        assertEquals(1, r.kept().size());
        assertTrue(r.warnings().get(0).startsWith("location-ambiguous"));
    }

    @Test void onsiteOutsideArgentinaRejected() {
        assertEquals("onsite-outside-argentina", reason(run(cfg(), ok("Java Dev", "Madrid, Spain", false))));
    }

    @Test void unknownRemoteStatusOutsideArgentinaWarns() {
        FilterResult r = run(cfg(), ok("Java Dev", "Madrid, Spain", null));
        assertEquals(1, r.kept().size());
        assertFalse(r.warnings().isEmpty());
    }

    @Test void juniorKeywordsRejected() {
        for (String t : List.of("Junior Java Developer", "Jr. Java Developer", "Java Trainee", "Java Intern",
                "Java Internship", "Entry Level Java Dev", "Entry-level Java Dev")) {
            FilterResult r = run(cfg(), ok(t, "Argentina", false));
            assertEquals(1, r.rejected().size(), t);
            assertTrue(reason(r).startsWith("seniority:"), t);
        }
    }

    @Test void juniorFromJobLevelRejected() {
        Job j = new Job("linkedin", "s1", "u", "Java Developer", "A", "Argentina", TODAY, false, null, "Internship", "d");
        assertEquals(1, run(cfg(), j).rejected().size());
    }

    @Test void juniorWordNotMatchedInsideOtherWords() {
        assertEquals(1, run(cfg(), ok("Java Internal Tools Developer", "Argentina", false)).kept().size());
    }

    @Test void seniorAndSemiSeniorKept() {
        assertEquals(2, run(cfg(), ok("Senior Java Developer", "Argentina", false),
                ok("Semi Senior Java Developer", "Argentina", false)).kept().size());
    }

    @Test void leadPrincipalStaffRejectedManagerHeadKeptWithWarning() {
        for (String t : List.of("Tech Lead Java", "Principal Engineer", "Staff Engineer")) {
            FilterResult r = run(cfg(), ok(t, "Argentina", false));
            assertEquals(1, r.rejected().size(), t);
            assertTrue(r.rejected().get(0).reason().startsWith("seniority:"), t);
        }
        for (String t : List.of("Engineering Manager", "Head of Java")) {
            FilterResult r = run(cfg(), ok(t, "Argentina", false));
            assertEquals(1, r.kept().size(), t);
            assertTrue(r.warnings().get(0).startsWith("seniority-question"), t);
        }
    }

    @Test void juniorAndSeniorMixKeptWithWarning() {
        FilterResult r = run(cfg(), ok("Junior / Senior Java Developer", "Argentina", false));
        assertEquals(1, r.kept().size());
        assertTrue(r.warnings().get(0).startsWith("seniority-ambiguous"));
    }

    @Test void excludeKeywordWholeWordCaseInsensitive() {
        AppConfig c = cfg(List.of("PHP", "C++"), true);
        Job inTitle = job("Java and php developer", "Argentina", false, TODAY, "d");
        Job inDesc = job("Java Developer", "Argentina", false, TODAY, "We also use C++ daily.");
        Job notWord = job("Java Developer", "Argentina", false, TODAY, "We love phpunit-free code.");
        FilterResult r = run(c, inTitle, inDesc, notWord);
        assertEquals(List.of(notWord), r.kept());
        assertEquals(2, r.rejected().size());
        assertTrue(reason(r).startsWith("excluded-keyword"));
    }

    static final String PT = "Estamos procurando uma pessoa desenvolvedora com experiência em Java para nossa equipe. "
            + "Você vai trabalhar com microsserviços e não precisa ser especialista, são bem-vindos todos. "
            + "A nossa empresa oferece trabalho remoto e também benefícios para os dos nossos colaboradores. "
            + "Nossa equipe é uma equipe pequena, você terá voz. Não é necessário inglês fluente, também uma boa experiência. "
            + "Nós trabalhamos com uma empresa que valoriza o trabalho das pessoas, das equipes e dos times.";

    @Test void clearlyPortugueseRejected() {
        FilterResult r = run(cfg(), job("Java Developer", "Argentina", false, TODAY, PT));
        assertEquals("language:pt", reason(r));
    }

    @Test void spanishAndEnglishKept() {
        String es = "Buscamos una persona con experiencia en Java para nuestro equipo. Trabajarás con los servicios de la empresa y "
                + "el equipo de producto, con una cultura de trabajo que valora a las personas. Es un puesto para el área de backend "
                + "con foco en el diseño de APIs y el trabajo con bases de datos, y con mucho aprendizaje por delante en el equipo.";
        String en = "We are looking for an engineer with experience in Java for our team. You will work with the services of the "
                + "company and the product team, and we are building a culture that is for the people who work with us. This is "
                + "a role for the backend area with a focus on the design of APIs and the work with databases, and you will have a lot to learn.";
        FilterResult r = run(cfg(), job("Java Dev", "Argentina", false, TODAY, es), job("Java Dev B", "Argentina", false, TODAY, en));
        assertEquals(2, r.kept().size());
    }

    @Test void shortOrNullDescriptionNeverRejectedForLanguage() {
        assertEquals(2, run(cfg(), job("A", "Argentina", false, TODAY, null), job("B", "Argentina", false, TODAY, "Ein Job")).kept().size());
    }

    @Test void inputNotMutatedAndMixedListIsClean() {
        List<Job> in = new java.util.ArrayList<>(List.of(
                ok("Senior Java Developer", "Argentina", false),
                ok("Junior Java Developer", "Argentina", false),
                ok("Java Dev", "Madrid, Spain", false)));
        List<Job> copy = List.copyOf(in);
        FilterResult r = HardFilters.apply(in, cfg(), TODAY);
        assertEquals(copy, in);
        assertEquals(1, r.kept().size());
        assertEquals(2, r.rejected().size());
        assertThrows(UnsupportedOperationException.class, () -> r.kept().add(in.get(0)));
    }
}
