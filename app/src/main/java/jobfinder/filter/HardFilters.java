package jobfinder.filter;

import jobfinder.config.AppConfig;
import jobfinder.source.Job;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic hard filters (no LLM). Order: dedup, freshness, location/remote, seniority,
 * exclude keywords, language. The first failing filter gives the reject reason.
 * When evidence is ambiguous the job is kept and a warning is emitted (never a silent guess).
 *
 * <h2>Heuristics</h2>
 * <ul>
 * <li><b>Dedup</b>: same source+id, same url, or same normalized (lowercase, no accents, alphanumerics only)
 *     title+company+location. First occurrence wins.</li>
 * <li><b>Freshness</b>: datePosted before today-maxAgeDays is rejected ("stale"); null date is kept with
 *     a "date-unknown" warning.</li>
 * <li><b>Location</b>: Argentina is detected by "argentina", "buenos aires", "caba", "cordoba", "rosario",
 *     "mendoza" or a ", AR" suffix. Remote is remoteHint==true or remote words (remote, remoto, teletrabajo,
 *     home office, work from home) in title/location. Another-region restriction needs text evidence in
 *     title/location/description: "<region> only", "solo/solamente/unicamente <region>", "must be located/based/
 *     reside in <region>", "residentes de/en <region>", "authorized to work in <region>". Regions: US, USA,
 *     United States, EEUU, Canada, UK, Europe, EU, EMEA, APAC, India, Spain, Mexico, etc. (Latin America is not
 *     listed because it includes Argentina.) If the text also mentions Argentina/LATAM/worldwide/anywhere the
 *     job is kept with a warning. Onsite outside Argentina is rejected only when remoteHint is explicitly false,
 *     the location is non-blank and no remote words appear; remoteHint null is kept with a warning.
 *     Remote with a non-Argentina location but no restriction text is kept with a warning.
 *     Onsite in Argentina follows location.acceptOnsiteInResidence (null: kept with a warning).</li>
 * <li><b>Seniority</b>: title/jobLevel whole words junior, jr, trainee, intern, internship, entry level are
 *     rejected, unless config.seniority lists "junior". Titles that also say senior/semi-senior/ssr are kept
 *     with a warning. lead, principal, staff, manager, head, director are never decided: kept with warning
 *     "seniority-question" (unless config.seniority lists the word).</li>
 * <li><b>Exclude keywords</b>: case-insensitive, whole-word (not preceded/followed by a letter or digit),
 *     searched in title and description.</li>
 * <li><b>Language</b>: stopword counts over the description for en, es, pt, fr, de, it. Rejected only when
 *     the description has at least 30 words, the strongest non-allowed language has at least 8 hits and at
 *     least 3 times the best allowed language's hits. Otherwise kept.</li>
 * </ul>
 */
public final class HardFilters {

    private HardFilters() {}

    private static final String REGIONS = "(?:the\\s+)?(?:us|usa|u\\.s\\.a?\\.?|united\\s+states(?:\\s+of\\s+america)?|"
            + "eeuu|ee\\.\\s?uu\\.?|estados\\s+unidos|canada|canad[aá]|uk|u\\.k\\.|united\\s+kingdom|reino\\s+unido|"
            + "europe|europa|eu|european\\s+union|emea|apac|india|spain|espana|españa|mexico|méxico|brazil|brasil|"
            + "germany|alemania|france|francia|australia|philippines|poland|portugal)";
    private static final Pattern RESTRICTION = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:" + REGIONS + "\\s*[-–]?\\s*(?:only|based\\s+only|residents\\s+only|candidates\\s+only)"
            + "|(?:solo|solamente|s[oó]lo|[uú]nicamente|exclusivo\\s+para)\\s+(?:en\\s+|para\\s+)?" + REGIONS
            + "|must\\s+(?:be\\s+(?:located|based|residing)|reside|live|be\\s+a\\s+resident)\\s+(?:in|within|of)\\s+" + REGIONS
            + "|(?:authorized|authorised|eligible|legally\\s+allowed)\\s+to\\s+work\\s+in\\s+" + REGIONS
            + "|residentes?\\s+(?:de|en)\\s+" + REGIONS
            + "|debe[sn]?\\s+(?:residir|vivir|estar\\s+ubicad[oa]s?)\\s+en\\s+" + REGIONS + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern OPEN_HINT = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(argentina|latam|latin\\s+america|latinoam[eé]rica|worldwide|anywhere|global|"
            + "mundo|cualquier\\s+parte)(?![\\p{L}\\p{N}])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ARGENTINA = Pattern.compile(
            "argentina|buenos aires|(?<![a-z])caba(?![a-z])|cordoba|rosario|mendoza|,\\s*ar\\s*$|^ar$|,\\s*ar\\s*,");
    private static final Pattern REMOTE = Pattern.compile(
            "(?<![a-z])(remote|remoto|remota|teletrabajo|home\\s*office|work\\s+from\\s+home|wfh)(?![a-z])");

    private static final Pattern JUNIOR = Pattern.compile(
            "(?<![a-z0-9])(junior|jr|trainee|intern|internship|entry[\\s-]+level)(?![a-z0-9])");
    private static final Pattern SENIOR = Pattern.compile(
            "(?<![a-z0-9])(senior|sr|semi[\\s-]?senior|ssr)(?![a-z0-9])");
    private static final Pattern LEAD = Pattern.compile(
            "(?<![a-z0-9])(lead|principal|staff|manager|head|director)(?![a-z0-9])");

    public static FilterResult apply(List<Job> jobs, AppConfig config, LocalDate today) {
        List<Job> kept = new ArrayList<>();
        List<Rejected> rejected = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (Job job : jobs) {
            if (isDuplicate(job, keys)) {
                rejected.add(new Rejected(job, "duplicate"));
                continue;
            }
            List<String> w = new ArrayList<>();
            String reason = check(job, config, today, w);
            if (reason != null) {
                rejected.add(new Rejected(job, reason));
            } else {
                kept.add(job);
                String label = job.source() + "/" + job.id() + " \"" + job.title() + "\"";
                for (String m : w) warnings.add(m + ": " + label);
            }
        }
        return new FilterResult(List.copyOf(kept), List.copyOf(rejected), List.copyOf(warnings));
    }

    private static String check(Job job, AppConfig cfg, LocalDate today, List<String> w) {
        if (job.datePosted() == null) {
            w.add("date-unknown");
        } else if (job.datePosted().isBefore(today.minusDays(cfg.maxAgeDays()))) {
            return "stale";
        }
        String r = location(job, cfg, w);
        if (r != null) return r;
        r = seniority(job, cfg, w);
        if (r != null) return r;
        r = excluded(job, cfg);
        if (r != null) return r;
        return language(job, cfg);
    }

    // ---- dedup ----
    private static boolean isDuplicate(Job j, Set<String> keys) {
        List<String> k = new ArrayList<>();
        if (j.source() != null && j.id() != null) k.add("id:" + j.source() + "|" + j.id());
        if (j.url() != null && !j.url().isBlank()) k.add("url:" + j.url().strip());
        if (j.title() != null && j.company() != null) {
            k.add("tcl:" + norm(j.title()) + "|" + norm(j.company()) + "|" + norm(j.location()));
        }
        boolean dup = false;
        for (String s : k) if (keys.contains(s)) dup = true;
        keys.addAll(k);
        return dup;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return lower(s).replaceAll("[^a-z0-9]+", " ").strip();
    }

    /** Lowercase and strip accents. */
    private static String lower(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    // ---- location ----
    private static String location(Job j, AppConfig cfg, List<String> w) {
        AppConfig.LocationRules rules = cfg.location();
        String loc = lower(j.location()).strip();
        String titleLoc = lower(j.title()) + " | " + loc;
        String all = String.join(" | ", nz(j.title()), nz(j.location()), nz(j.description()));
        boolean inArg = !loc.isEmpty() && ARGENTINA.matcher(loc).find();
        boolean remote = Boolean.TRUE.equals(j.remoteHint()) || REMOTE.matcher(titleLoc).find();

        Matcher m = RESTRICTION.matcher(all);
        if (m.find() && (rules == null || rules.rejectRemoteRestrictedToOtherRegions())) {
            if (OPEN_HINT.matcher(all).find() || inArg) {
                w.add("location-ambiguous (restriction text \"" + m.group().strip() + "\" but also open/Argentina text)");
                return null;
            }
            return "remote-restricted:" + m.group().strip().toLowerCase(Locale.ROOT);
        }
        if (inArg) {
            if (remote) return null;
            Boolean onsite = rules == null ? null : rules.acceptOnsiteInResidence();
            if (Boolean.FALSE.equals(onsite)) return "onsite-not-accepted";
            if (onsite == null) w.add("onsite-undecided");
            return null;
        }
        if (remote) {
            if (!loc.isEmpty()) w.add("location-ambiguous (remote, listed location \"" + j.location() + "\", no restriction text)");
            return null;
        }
        if (loc.isEmpty()) {
            w.add("location-unknown");
            return null;
        }
        if (Boolean.FALSE.equals(j.remoteHint())) return "onsite-outside-argentina";
        w.add("location-ambiguous (not in Argentina, remote status unknown)");
        return null;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // ---- seniority ----
    private static String seniority(Job j, AppConfig cfg, List<String> w) {
        String text = lower(j.title()) + " | " + lower(j.jobLevel());
        List<String> wanted = cfg.seniority() == null ? List.of() : cfg.seniority().stream().map(HardFilters::lower).toList();
        boolean junior = wanted.stream().anyMatch(s -> s.contains("junior") || s.equals("jr"));
        Matcher jm = JUNIOR.matcher(text);
        if (!junior && jm.find()) {
            if (SENIOR.matcher(text).find()) {
                w.add("seniority-ambiguous (title mixes junior and senior words)");
            } else {
                return "seniority:" + jm.group(1).replaceAll("[\\s-]+", " ");
            }
        }
        Matcher lm = LEAD.matcher(text);
        if (lm.find() && wanted.stream().noneMatch(s -> s.contains(lm.group(1)))) {
            w.add("seniority-question (\"" + lm.group(1) + "\" role: owner to decide)");
        }
        return null;
    }

    // ---- exclude keywords ----
    private static String excluded(Job j, AppConfig cfg) {
        if (cfg.excludeKeywords() == null) return null;
        String text = nz(j.title()) + "\n" + nz(j.description());
        for (String kw : cfg.excludeKeywords()) {
            if (kw == null || kw.isBlank()) continue;
            Pattern p = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(kw.strip()) + "(?![\\p{L}\\p{N}])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            if (p.matcher(text).find()) return "excluded-keyword:" + kw.strip();
        }
        return null;
    }

    // ---- language ----
    private static final Map<String, Set<String>> STOPWORDS = new LinkedHashMap<>();
    static {
        STOPWORDS.put("en", Set.of("the", "and", "of", "to", "in", "for", "with", "you", "we", "our", "is", "are",
                "will", "your", "that", "this", "have", "experience", "team", "work"));
        STOPWORDS.put("es", Set.of("el", "la", "los", "las", "de", "y", "en", "para", "con", "por", "una", "un", "que",
                "del", "se", "nuestro", "nuestra", "experiencia", "equipo", "trabajo", "sobre", "como"));
        STOPWORDS.put("pt", Set.of("nao", "voce", "voces", "sao", "tambem", "experiencia", "trabalho", "empresa",
                "nossa", "nosso", "equipe", "uma", "dos", "das", "para"));
        STOPWORDS.put("fr", Set.of("les", "des", "une", "nous", "vous", "est", "dans", "avec", "pour", "sont", "notre",
                "votre", "equipe", "poste", "entreprise"));
        STOPWORDS.put("de", Set.of("und", "der", "die", "das", "mit", "fur", "wir", "sie", "ist", "nicht", "ein",
                "eine", "bei", "oder", "unser", "unsere"));
        STOPWORDS.put("it", Set.of("della", "sono", "nostra", "nostro", "anche", "che", "gli", "una", "per", "squadra",
                "azienda", "lavoro"));
    }

    private static String language(Job j, AppConfig cfg) {
        if (j.description() == null || cfg.languages() == null || cfg.languages().isEmpty()) return null;
        String[] words = lower(j.description()).split("[^a-z]+");
        if (words.length < 30) return null;
        Map<String, Integer> hits = new LinkedHashMap<>();
        for (String lang : STOPWORDS.keySet()) hits.put(lang, 0);
        for (String word : words) {
            for (var e : STOPWORDS.entrySet()) {
                if (e.getValue().contains(word)) hits.merge(e.getKey(), 1, Integer::sum);
            }
        }
        Set<String> allowed = new HashSet<>();
        for (String l : cfg.languages()) allowed.add(l.toLowerCase(Locale.ROOT));
        int bestAllowed = 0;
        String other = null;
        int bestOther = 0;
        for (var e : hits.entrySet()) {
            if (allowed.contains(e.getKey())) bestAllowed = Math.max(bestAllowed, e.getValue());
            else if (e.getValue() > bestOther) { bestOther = e.getValue(); other = e.getKey(); }
        }
        if (other != null && bestOther >= 8 && bestOther >= 3 * bestAllowed) return "language:" + other;
        return null;
    }
}
