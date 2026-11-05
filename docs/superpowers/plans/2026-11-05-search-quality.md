# Search Quality Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stop the agent from surfacing hybrid/onsite jobs in places the owner cannot live, stop it surfacing jobs that require Portuguese, and search more boards than LinkedIn.

**Architecture:** All fixes extend the existing pipeline stages. Location and language are deterministic rules in `HardFilters` driven by new config keys. Multi-source reuses the JobSpy sidecar: one `JobSource` per configured site, one `Collector` pass per source, merged before the existing dedup.

**Tech Stack:** Java (Maven, JUnit 5, Jackson 3 snake_case config), Python JobSpy sidecar.

**Spec:** `specs/02-decisions-log.md` D21/D22 (location rule: this plan supersedes D22 for onsite/hybrid), `specs/08-open-questions.md` Q10 (Indeed/Glassdoor). Owner statements this session, 2026-11-05: prefers remote; lives in Provincia de Buenos Aires but cannot live in any Buenos Aires town; only Rosario works for hybrid/onsite; not a Portuguese speaker; wants Indeed, Glassdoor and similar boards.

## Global Constraints

- D33: no paid services, no proxy. Failed searches are tolerated and recorded, never fatal unless every source fails.
- Hard filters stay deterministic (no LLM). Ambiguous evidence keeps the job and adds a warning, never a silent guess.
- Config keys are snake_case in `config/config.json`; new record components are nullable and null means legacy behavior, so existing tests keep passing.
- Reject reasons are lowercase `kind:detail` strings, as in `HardFilters` today.

## Review Focus

- Remote job whose listed location is Buenos Aires/CABA (remote hint true): must be kept.
- Job listed in CABA with description "100% remote" but remote hint false: kept, not rejected as onsite.
- "Hybrid" in title or location while remote hint is true: treated as hybrid, rejected outside Rosario.
- "Portuguese is a plus / nice to have / deseable" : kept with a `portuguese-optional` warning, never rejected.
- "Portugués avanzado" / "Fluent in Portuguese" / "Portuguese required" in a Spanish or English description, or Portuguese in the title: rejected `language-required:pt`.
- The same job seen on LinkedIn and Indeed: one survives (existing title+company+location dedup), and one source blocked (e.g. Glassdoor 429) does not abort the run.

---

### Task 1: Reject postings that require Portuguese

Root cause: `HardFilters.language` only measures the dominant language of the description via stopwords. A Spanish or English posting that says "Portuguese required" passes.

**Files:**
- Modify: `app/src/main/java/jobfinder/filter/HardFilters.java` (language step, class Javadoc)
- Test: `app/src/test/java/jobfinder/filter/HardFiltersTest.java`

**Interfaces:**
- Consumes: `Job.title()`, `Job.description()`, `AppConfig.languages()` (list of codes; `"pt"` present means Portuguese is allowed).
- Produces: reject reason `language-required:pt`; warning `portuguese-optional`. `HardFilters.apply` signature unchanged.

- [ ] **Step 1: Write failing tests** in `HardFiltersTest` (use `job(...)`/`run(...)` helpers, cfg languages are `es,en`):
  - `portugueseRequiredEnglishRejected`: description "Fluent in Portuguese is required." and `portugueseRequiredSpanishRejected`: "Se requiere portugués avanzado." both assert `reason(r)` equals `"language-required:pt"`.
  - `portugueseInTitleRejected`: title "Java Developer (Portuguese speaking)", short description, rejected `language-required:pt`.
  - `portugueseNiceToHaveKeptWithWarning`: "Portuguese is a plus." and "Portugués deseable." kept; `r.warnings().get(0)` starts with `"portuguese-optional"`.
  - `portugueseAllowedWhenConfigured`: config languages `es,en,pt` keeps the "required" job.
  - `noPortugueseMentionUnaffected`: existing `spanishAndEnglishKept` still passes.
- [ ] **Step 2: Run** `cd app && mvn -q test -Dtest=HardFiltersTest` — Expected: new tests FAIL.
- [ ] **Step 3: Implement** in `HardFilters.java`: `private static String portuguese(Job j, AppConfig cfg, List<String> w)` called first inside `language(...)`, taking the warnings list (add the parameter to `language` and pass `w` from `check`). Skip when `cfg.languages()` contains `"pt"`. On the accent-stripped lowercase `title + " | " + description`, find each `portugu[e]s|portuguese` mention and inspect a ±60 character window: optional markers (`plus|nice to have|bonus|deseable|valorable|ventaja|preferible|a plus|not required|no excluyente`) → warning only; otherwise requirement markers (`required|requerid|fluent|fluen|native|nativo|advanced|avanzad|mandatory|obligatori|excluyente|must|se requiere|speaking|hablante|bilingue|dominio|c1|c2`) → return `"language-required:pt"`. A bare mention with neither marker is kept silently. The optional check wins over the requirement check inside the same window.
- [ ] **Step 4: Run** the same command — Expected: all `HardFiltersTest` PASS.
- [ ] **Step 5: Update** the "Language" bullet in the class Javadoc, then commit: `git add app && git commit -m "fix: reject postings requiring Portuguese"`.

---

### Task 2: Hybrid/onsite only in the owner's cities

Root cause: `HardFilters.ARGENTINA` matches "buenos aires" (and every Argentine city), then `location()` accepts any Argentine onsite job when `accept_onsite_in_residence` is true. `Collector.buildQueries` also searches onsite jobs with location "Argentina", pulling mostly CABA results.

**Files:**
- Modify: `app/src/main/java/jobfinder/config/AppConfig.java` (add `List<String> onsiteCities` as the last `LocationRules` component)
- Modify: `app/src/main/java/jobfinder/filter/HardFilters.java` (`location`, Javadoc)
- Modify: `app/src/main/java/jobfinder/source/Collector.java:33-42` (`buildQueries`)
- Modify: `config/config.json` (add `"onsite_cities": ["Rosario"]` inside `location`)
- Modify tests: `HardFiltersTest.cfg(...)` passes `null` as the new component; `CollectTest`
- Test: `HardFiltersTest`, `CollectTest`

**Interfaces:**
- Consumes: `Job.location()`, `Job.title()`, `Job.description()`, `Job.remoteHint()`.
- Produces: `AppConfig.LocationRules#onsiteCities() : List<String>` (null or empty = legacy behavior: any Argentine city accepted). Reject reason `onsite-outside-home-area`. A second helper in tests: `cfgRosario()` returning `cfg()` with `onsiteCities = List.of("Rosario")`.

- [ ] **Step 1: Write failing tests** in `HardFiltersTest` using `cfgRosario()`:
  - `hybridBuenosAiresRejected`: `ok("Java Dev (Hybrid)", "Buenos Aires, Argentina", false)` → `onsite-outside-home-area`.
  - `onsiteCabaRejected`: location "Capital Federal, Buenos Aires, Argentina", remote false → rejected.
  - `hybridRosarioKept`: location "Rosario, Santa Fe, Argentina", remote false → kept.
  - `remoteBuenosAiresKept`: remote true, location "Buenos Aires, Argentina" → kept.
  - `hybridWordWinsOverRemoteHint`: title "Java Dev - Hybrid", remote true, location "Buenos Aires, Argentina" → rejected.
  - `fullyRemoteDescriptionKept`: location "Buenos Aires, Argentina", remote false, description "This is a 100% remote position." → kept.
  - `nullCitiesIsLegacy`: existing `argentinaOnsiteAcceptedWhenConfigured` passes unchanged.
  In `CollectTest.buildsRemoteAndArgentinaQueriesWithOwnFreshnessWindow`: assert `qs.get(1).location()` equals `"Rosario, Argentina"`.
- [ ] **Step 2: Run** `cd app && mvn -q test -Dtest='HardFiltersTest,CollectTest'` — Expected: FAIL (compile error until the record gains the component, then assertion failures).
- [ ] **Step 3: Implement** the record component and update `cfg(...)` in the test. In `location(...)`: add `HYBRID = (hybrid|hibrido|hibrida|semipresencial)` tested on `titleLoc`, and `FULL_REMOTE = (100%\s*(remote|remoto)|fully\s+remote|full\s+remote|totalmente\s+remoto)` tested on the lowercased description. Compute `remote = (remoteHint==true || REMOTE(titleLoc) || FULL_REMOTE(desc)) && !HYBRID(titleLoc)`. In the `inArg && !remote` branch, when `onsiteCities` is non-empty and no entry matches `loc` as a whole word (use `lower()` on both), return `"onsite-outside-home-area"` before the existing `acceptOnsiteInResidence` handling.
- [ ] **Step 4: Implement** `Collector.buildQueries`: the second query per role loops `c.location().onsiteCities()` (fallback `List.of("Argentina")` when null/empty) with location `city + ", Argentina"`; the remote query keeps location "Argentina".
- [ ] **Step 5: Update** `config/config.json` and the Javadocs. Add a decision line to `specs/02-decisions-log.md`: D34, 2026-11-05, owner: remote preferred; onsite/hybrid only in Rosario; supersedes D22.
- [ ] **Step 6: Run** `cd app && mvn -q test` — Expected: all PASS. Commit: `git commit -am "fix: restrict hybrid/onsite to configured cities"`.

---

### Task 3: Search Indeed and Glassdoor (not only LinkedIn)

JobSpy already supports these sites; the repo hard-codes LinkedIn in three places: `sidecar/run_jobspy.py` (`site_name=["linkedin"]`), class `JobSpyLinkedInSource` (name "linkedin"), and `JobFinderApp`/`Pipeline` (one `JobSource`). `config.sources` already exists but is never read for sources.

**Files:**
- Modify: `sidecar/run_jobspy.py`
- Rename: `app/src/main/java/jobfinder/source/JobSpyLinkedInSource.java` → `JobSpySource.java`
- Modify: `app/src/main/java/jobfinder/source/Collector.java` (`buildQueries`), `app/src/main/java/jobfinder/pipeline/Pipeline.java`, `app/src/main/java/jobfinder/JobFinderApp.java`
- Modify: `app/src/main/java/jobfinder/config/AppConfig.java` (add `List<String> simpleTerms` as the last component), `config/config.json`
- Modify tests: `HardFiltersTest.cfg`, `PipelineTest`, `CollectTest` (constructor changes)
- Test: `CollectTest`, `PipelineTest`

**Interfaces:**
- Produces: `JobSpySource(String site, List<String> command)` with `name()` returning `site`; sidecar input gains `"site"` and `"country"`. `Collector.buildQueries(AppConfig c, String source, int resultsWanted, boolean fetchDescription, String jobType) : List<SearchQuery>` (LinkedIn keeps the boolean `target_roles` terms; every other source uses `simple_terms`, each as plain `<term>` and `<term> remote`). `Pipeline(AppConfig, Profile, List<JobSource>, ScoreModel, Path, RunMetrics)`. Config `"sources": ["linkedin","indeed","glassdoor"]`, `"simple_terms": ["Java backend developer"]`.

- [ ] **Step 1: Live spike first (no code kept).** In `sidecar`, run the existing venv with `site_name=["indeed"]` then `["glassdoor"]`, `country_indeed="Argentina"`, term "Java backend developer", `results_wanted=10`, `hours_old=168`. Record in `spike/jobspy/REPORT.md`: rows returned, description filled?, `is_remote` filled?, errors. Expected: Indeed returns rows; Glassdoor may 403/429 (D33 means no proxy). If a site returns zero usable rows, drop it from the plan's config and note why.
- [ ] **Step 2: Write failing tests.** `CollectTest`: `nonLinkedinUsesSimpleTerms` (source "indeed" → search terms are `Java backend developer` and `Java backend developer remote`, none contain `AND`), `linkedinKeepsBooleanTerms` (existing assertions). `PipelineTest`: `oneSourceFailingDoesNotAbortRun` (two fake sources, the first throws `IOException` on every search; run completes with the second source's jobs) and `crossSourceDuplicateKeptOnce` (same title/company/location from two sources → one kept).
- [ ] **Step 3: Run** `cd app && mvn -q test` — Expected: FAIL.
- [ ] **Step 4: Implement** the sidecar change: `site_name=[q["site"]]`, plus `country_indeed=q.get("country", "Argentina")`. Rename the class, add the `site` constructor parameter, send `site` and `country` in the input map. In `Pipeline.run`: one `Collector` per source, concatenate the jobs, catch the `IOException` thrown when a single source fails entirely and log a warning; rethrow only if every source failed. `JobFinderApp`: build one `RetryingSource(new JobSpySource(site, cmd))` per entry of `config.sources()`.
- [ ] **Step 5: Run** `cd app && mvn -q test` — Expected: all PASS.
- [ ] **Step 6: Live run and commit.** `cd app && mvn -q spring-boot:run -Dspring-boot.run.arguments=run` (needs `OLLAMA_MODEL`); confirm `data/runs/` metrics show one `search` line per source and the shortlist has jobs from more than one `source`. Update README source list. Commit: `git commit -am "feat: search Indeed and Glassdoor via JobSpy"`.

---

## Out of scope (say if you want them)

- Boards JobSpy lacks (Bumeran, Computrabajo, Get on Board, Remote OK, We Work Remotely): need a new sidecar or an API client per board. Ask for a follow-up plan.
- Prompt change so the LLM also penalizes Portuguese: skipped, the hard filter covers it.

## Self-review

- Spec coverage: PBA/CABA location (Task 2), Portuguese (Task 1), more sources (Task 3); all three owner complaints mapped.
- Types: `onsiteCities` (Task 2) and `simpleTerms` (Task 3) both extend records, and each task updates `HardFiltersTest.cfg` — apply Task 2's edit first, Task 3 adds the second `null`.
- Open risk: Glassdoor and Indeed Argentina coverage is unverified until Task 3 Step 1.
