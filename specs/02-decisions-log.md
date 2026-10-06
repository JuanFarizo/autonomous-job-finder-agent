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
| D14 | LLM provider: Ollama Cloud free tier via API key. Anthropic API is NOT available (owner has Claude Pro, which gives no API key) | DECIDED | 2026-10-06 | Owner (Q3). Supersedes first D14 (Claude primary). Claude API only if owner buys API credits later |
| D15 | Java stack: Spring Boot 4 + Spring AI 2.x as LLM client and structured output | DECIDED | 2026-10-06 | Owner (Q2). Fit still validated in Phase 0 spike; fallback `anthropic-java` only if validation fails, with owner approval |
| D16 | Model: an Ollama Cloud free-tier / starter model, to be chosen by testing in Phase 0 | DECIDED (provider), PENDING (model) | 2026-10-06 | Supersedes Sonnet 5.5 choice. Ollama pricing page: free plan has starter models, starter credits not rolled over, 1 concurrent request |
| D17 | Phase 0 spike runs from owner's own IP, no proxy, small result cap | DECIDED | 2026-10-06 | Owner: avoid proxies, keep it easy, avoid LinkedIn block |
| D18 | career-ops port scope: trimmed (blocks A and B, 5-dimension score, E, plus grounding rule). Not the full 120 KB prompt | DECIDED | 2026-10-06 | Owner (Q1 follow-up). Attribution to career-ops (MIT) required; do not name product "career-ops" |
| D19 | Java 25.0.4 (amzn) and Maven 3.9.10, pinned in `.sdkmanrc` | DECIDED | 2026-10-06 | Owner |
| D20 | Seniority: semi-senior and senior. Posting languages: Spanish, English | DECIDED | 2026-10-06 | Owner (Q5) |
| D21 | Location rule: accept located in Argentina, remote open to Argentina, or remote from anywhere. Reject remote restricted to other countries/regions (owner will not relocate) | DECIDED | 2026-10-06 | Owner (Q5). Onsite/hybrid in Argentina: not stated, kept null, asked again |
| D22 | Accept onsite/hybrid in Argentina as well as remote | DECIDED | 2026-10-06 | Owner |
| D23 | Target role title: "Java Developer" only | DECIDED | 2026-10-06 | Owner (Q5) |
| D24 | Owner-stated facts recorded as proof points (Java 8 to 25, OAuth authorization-server lambda, English working level). Exclude keywords and min score: skipped for now | DECIDED | 2026-10-06 | Owner (Q11 path: grounded claims only) |
| D25 | Java-Python bridge: Java launches the sidecar as a subprocess, JSON on stdin, JSON on stdout (Q4) | DECIDED | 2026-10-06 | Owner accepted recommendation |
| D26 | Search query: `Java AND (Developer OR Engineer) AND (Backend OR Web)` | DECIDED | 2026-10-06 | Owner. Whether LinkedIn honors the boolean operators is UNVERIFIED until a live run |
