# T2: career-ops portability (subagent report, key facts re-verified by orchestrator)
- License MIT, "Copyright (c) 2026 Santiago Fernandez de Valderrama" (verified LICENSE). TRADEMARK.md: do not use the name "career-ops" for our product; "based on career-ops" with attribution is allowed.
- Prompts are plain Markdown: modes/_shared.md (27 KB), modes/oferta.md (93 KB), batch/batch-prompt.md. Repo ships openai-eval.mjs, a standalone API runner (verified file exists), so porting to an LLM API call is proven by its own design. Not executed by us.
- Score is holistic 1-5 over five dimensions, no formula. Blocks A-G always, H only if score >= 4.5.
- Grounding rule verified in modes/_shared.md lines 28 and 33: never hardcode metrics, keywords reformulated never fabricated. Primary truth = cv.md + article-digest.md + profile; story bank is lower trust. Enforcement scripts (verify-cv-facts.mjs, story-provenance-check.mjs) are in Node, not ported by prompts alone.
- Pinned commit inspected: 24745c5f8a6b2ee56d7c25176d05c4dfe97c5b3b (2026-10-05).
- Spec corrections needed: grounding wording; blocks A-G plus conditional H; stars/forks/commits unverified (shallow clone); repo may have moved to career-ops-hq/career-ops.
