# 07. Risks

| Risk | Detail | Mitigation (proposed) |
|---|---|---|
| LinkedIn blocking / rate limits | JobSpy README: LinkedIn rate limits around the 10th page per IP; proxies basically a must | Short freshness window (fewer pages), backoff, store raw data, settle proxy approach in Phase 7 |
| Terms of service | LinkedIn's terms generally prohibit automated scraping (general knowledge, not re-verified in this session). AIHawk's creator was reportedly restricted by LinkedIn | Prefer no-login collection (JobSpy); avoid the owner's personal session; owner accepts residual risk |
| Scraper breakage | Sites change; selectors and endpoints break | Source interface isolates the impact; verify JobSpy maintenance in Phase 0 |
| JobSpy maintenance | Verified active in Phase 0 (last push 2026-10-02, v1.2.0) | Re-check in Phase 7 |
| Invented CV claims | LLMs can hallucinate skills | Grounding rule: only use provable proof points (C8) |
| Cost | LLM scoring on many jobs | Hard filters first; score only survivors |
| Stale or ghost postings | Postings may be old or already closed | Own date check; consider a liveness check later (career-ops has one for its scanner) |
| Validation limits | "Verified" means repo evidence, not execution | Phase 0 spike runs the real tools |
