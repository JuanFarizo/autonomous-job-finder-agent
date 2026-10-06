# 05. Phases (logical order, no timeline)

Each phase depends only on the ones before it. Phases 1 and 2 are independent and can run in parallel.

## Phase 0: Spike and decisions
- Run JobSpy against LinkedIn from the owner's environment with `hours_old=168`; inspect real output.
- Read career-ops' evaluation prompts and license; confirm they can be ported.
- Research Java libraries for LLM calls and structured output.
- Confirm JobSpy's current maintenance status.
- **Exit:** a sample of real fresh LinkedIn postings and a chosen LLM library.

## Phase 1: Candidate profile and configuration
- Structured profile from the owner's CV: skills, experience, proof points.
- Proof-points file backing the grounding rule.
- Config file (see `06-configuration.md`).
- **Exit:** profile and config that the rest of the pipeline reads.

## Phase 2: LinkedIn collection
- Java builds queries from profile/config and calls the JobSpy sidecar.
- Full descriptions require the extra fetch (one request per job).
- Store raw, then convert to the internal Job model.
- Define the source interface.
- **Decide:** Java-Python bridge.
- **Exit:** raw LinkedIn jobs saved locally.

## Phase 3: Normalization and hard filters
- Dedup, freshness (own date check), location/remote, seniority, exclusion keywords.
- **Exit:** a clean list where every job meets the hard constraints.

## Phase 4: Scoring and ranking
- LLM scoring with the ported rubric, structured output, written reason.
- Minimum-score threshold; sort by score then freshness.
- **Exit:** a ranked shortlist validated by eye against known good and bad fits.

## Phase 5: Capability highlighting and CV tailoring
- Select matching proof points per job; draft tailored summary and bullets under the grounding rule.
- **Decide:** output format (career-ops Playwright route or Java-side).
- **Exit:** a tailored CV per top job for owner review.

## Phase 6: Output and tracking
- Shortlist report; tracker with statuses (new, reviewed, applied manually).
- Seen-jobs store so a posting is not shown twice.
- **Exit:** a daily output the owner can act on.

## Phase 7: Scheduling and robustness
- Periodic runs, backoff on rate limits, logging, safe failure when blocked.
- Settle the proxy question.
- **Exit:** unattended runs that fail safely.

## Phase 8: Additional sources (later)
- Research Indeed and Glassdoor API/MCP options: maintenance, authentication, terms.
- Note: JobSpy already supports Indeed directly, so an MCP may not be needed for Indeed. Verify rather than assume.
- Optionally add the career-ops company career-page scanner.
- Integrate through the Phase 2 source interface.
- **Exit:** validated extra sources integrated.
