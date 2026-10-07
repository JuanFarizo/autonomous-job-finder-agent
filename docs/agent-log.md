# Agent log

## Phase 0 (2026-10-06)
| Task | Who | Verified by orchestrator | Result |
|---|---|---|---|
| T1 JobSpy LinkedIn run | Orchestrator | Ran it; sample.json inspected | Accepted. 15 rows, no block. is_remote unreliable, description empty (fetch off) |
| T2 career-ops inspection | Subagent | Re-checked LICENSE, grounding quotes, openai-eval.mjs, commit SHA | Accepted. MIT, portable. Subagent could not write its report file; orchestrator wrote it |
| T3 JobSpy maintenance | Orchestrator | GitHub API and PyPI | Accepted. Active, v1.2.0 on 2026-10-02 |
| T4 Spring Boot 4 + Spring AI | Subagent | Re-ran `mvn verify` without keys: 2 tests skipped, BUILD SUCCESS | Partly accepted. Compiles and context starts. No live LLM call yet (keys not exported) |

No subagent violated a ground rule. No secrets in the repo.
| A3 Phase 6 tracker | Subagent (worktree) | Re-ran mvn test (11 total, green), grepped for apply/submit code: none, only track dirs changed | Accepted and merged. First launch with harness worktree isolation failed (worktree built from initial commit); relaunched in manual worktrees |
| A2 Phase 4 scorer | Subagent (worktree) | Re-ran mvn test (14 green), read prompt: attribution, grounding and untrusted-input rules present, MIT license copied | Accepted and merged. Placeholder DEFAULT_MIN_SCORE=3.5 is NOT an owner decision; needs confirmation. No live LLM call |
| A1 Phase 3 filters | Subagent (worktree) | Re-ran full mvn test after merge, only filter dirs changed | Accepted and merged. Heuristics chosen by agent (language threshold, city list, ambiguous cases) flagged to owner |
| Observability | Orchestrator | RunMetrics + tests | Added: search stats, result-field fill, funnel, LLM tokens (provider or estimated) |
| Phase 5 tailoring + PDF | Orchestrator | Real PDF rendered from real profile, text extracted back (2 pages); grounding tests | Done. openhtmltopdf validated by running |
| Pipeline, Ollama adapter, retry | Orchestrator | 55 tests green; app starts with Spring AI context | Done with fakes. Live LinkedIn and LLM calls NOT run (owner: continue without live tests) |
