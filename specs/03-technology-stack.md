# 03. Technology stack

## Chosen / proposed

| Layer | Technology | Status | Notes |
|---|---|---|---|
| Orchestration, filtering, scoring, tailoring logic | Java | DECIDED | Owner's language; tiebreaker rule |
| LinkedIn collection | JobSpy (Python library) as a sidecar | DECIDED (architecture), VERIFIED (project) | https://github.com/speedyapply/JobSpy |
| Evaluation prompts and grounding rule | Ported from career-ops (MIT) | PROPOSED | https://github.com/santifer/career-ops |
| Company career-page scanner | career-ops scanner | PENDING | Optional later source; code not inspected |
| CV PDF rendering | career-ops HTML+Playwright route, or a Java-side library | PENDING | Decide in Phase 5 |
| Java app framework, LLM client, structured output | Spring Boot 4 + Spring AI 2.x | DECIDED (D15), validate in Phase 0 | Candidate fallback: `anthropic-java` |
| Java to Python bridge | Subprocess vs local service | PENDING | Decide in Phase 2 |
| Indeed / Glassdoor | API or MCP | PENDING | Research in Phase 8 |

## JobSpy: verified facts
- Repo: 4.4k stars, 878 forks, 368 commits, MIT license, Python >= 3.10.
- Scrapes LinkedIn, Indeed, Glassdoor, ZipRecruiter and others into a normalized table.
- `hours_old` filters by posting age (7 days = 168 hours). Hour-accurate for LinkedIn and Indeed; Glassdoor rounds up to days.
- Indeed filters by when the job was added to Indeed, which can be later than the employer's publish date, so the agent must also check `date_posted` itself.
- `fetch_description` fetches each job's page for the full description, adding one request per job.
- LinkedIn is the most restrictive: it usually rate-limits around the 10th page from one IP. Proxies are described as basically a must.
- Results are capped at around 1000 per search per board.
- Google Jobs is currently unavailable (per README FAQ).
- Indeed supports Argentina via `country_indeed`.
- Reported by a third-party test: LinkedIn and Indeed returned results while Glassdoor returned a 400 error.
- Maintenance checked 2026-10-06: last push 2026-10-02, release v1.2.0 (PyPI python-jobspy 1.2.0), 25 open issues, not archived. The earlier 2026-02-18 date was outdated.

## career-ops: verified facts
- Repo: 69.1k stars, 13.1k forks, 1,693 commits, MIT license, tests folder present. (Other sources quoted an outdated 9.1K stars.)
- Evaluates a job into blocks A-H with a global 1-5 score; generates ATS-tailored CVs; tracks applications.
- Draft-only: it never submits or clicks anything.
- Its scanner covers company ATS feeds (Greenhouse, Ashby, Lever), NOT LinkedIn search. For LinkedIn it only evaluates a pasted URL.
- It is prompt/skill-driven and runs inside an AI coding CLI; orchestration is not reusable as-is for a standalone Java agent.
- Grounding rule (inspected): user-facing claims may only come from cv.md, article-digest.md and the profile; keywords are reformulated, never fabricated. Story-bank figures are lower trust and need provenance. Enforcement scripts are Node, not in the prompts.
- Inspected in Phase 0 at commit 24745c5: MIT, prompts are portable Markdown, score is holistic 1-5 (no formula), blocks A-G always and H only when score >= 4.5. See spike/career-ops/REPORT.md. TRADEMARK.md forbids the name "career-ops" for derived products. Stars/forks/commits not re-verified.

## Left out by default
- `stickerdaniel/linkedin-mcp-server`: uses the owner's logged-in LinkedIn session via browser automation. Partially verified (third-party listings and CI badges; repo not opened directly). See D10.
