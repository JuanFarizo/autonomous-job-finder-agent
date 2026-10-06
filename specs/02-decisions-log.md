# 02. Decisions log

| ID | Decision | Status | Date | Notes |
|---|---|---|---|---|
| D1 | Application step is discovery only | DECIDED | 2026-10-06 | Owner applies manually |
| D2 | Primary source is LinkedIn | DECIDED | 2026-10-06 | |
| D3 | Other boards later, via API/MCP if easy to connect | DECIDED | 2026-10-06 | Phase 8, requires its own research |
| D4 | Stack by best fit, Java as tiebreaker | DECIDED | 2026-10-06 | |
| D5 | Architecture "Option A": Java orchestrates, JobSpy runs as Python sidecar | DECIDED | 2026-10-06 | Owner: "Option A is the best" |
| D6 | Reuse selected parts of career-ops (evaluation prompts, grounding rule) | DECIDED | 2026-10-06 | Owner confirmed reuse (Q1). Portability (license, prompt content) still checked in Phase 0; report back if blocked |
| D7 | Max job age default 7 days, configurable | DECIDED | 2026-10-06 | |
| D8 | Start with LinkedIn only; extra sources in a later phase | DECIDED | 2026-10-06 | |
| D9 | Logical order of phases, no timeline | DECIDED | 2026-10-06 | See `05-phases.md` |
| D10 | Do NOT use the cookie-based LinkedIn MCP server by default | PROPOSED | 2026-10-06 | Uses the owner's real session; account risk |
| D11 | Source interface defined from the start so new sources plug in later | PROPOSED | 2026-10-06 | See `04-architecture.md` |
| D12 | Storage: plain JSON files; SQLite only if a database becomes mandatory | DECIDED | 2026-10-06 | Owner: keep it simple (Q8) |
| D13 | Search intent: remote contractor roles, Java Senior Web Developer; from Argentina or fully remote | DECIDED | 2026-10-06 | Owner (Q5). Exact terms and locations detailed in Phase 1 from owner's LinkedIn profile |
| D14 | Primary LLM provider: Anthropic Claude. Ollama (free tier) available as secondary | DECIDED | 2026-10-06 | Owner (Q3). Budget and exact model still open |
| D15 | Java stack: Spring Boot 4 + Spring AI 2.x as LLM client and structured output | DECIDED | 2026-10-06 | Owner (Q2). Fit still validated in Phase 0 spike; fallback `anthropic-java` only if validation fails, with owner approval |
| D16 | LLM models: Claude Sonnet 5.5 (primary); Ollama Cloud free tier (secondary). Budget cap not yet given | DECIDED | 2026-10-06 | Owner (Q3). Refines D14 |
| D17 | Phase 0 spike runs from owner's own IP, no proxy, small result cap | DECIDED | 2026-10-06 | Owner: avoid proxies, keep it easy, avoid LinkedIn block |
