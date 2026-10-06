# 04. Architecture (draft)

## Pipeline

```
Profile + Config
      |
      v
[Collector]  --> raw jobs (LinkedIn via JobSpy sidecar)
      |
      v
[Normalizer] --> internal Job model
      |
      v
[Hard filters] --> dedup, freshness, location/remote, seniority, exclusions (deterministic, no LLM)
      |
      v
[Scorer]     --> LLM fit score + written reason (structured output)
      |
      v
[Ranker]     --> threshold, then sort by score, then freshness
      |
      v
[Tailor]     --> per-job highlights + CV content, grounded in proof points
      |
      v
[Output/Tracker] --> shortlist report, status tracking, seen-jobs store
```

## Design rules
1. **Hard filters before the LLM.** Only survivors of deterministic filtering are scored, to control cost.
2. **Source interface (PROPOSED).** Each source implements one interface that returns raw jobs. LinkedIn is the first implementation; Indeed, Glassdoor or company feeds plug in later without changing downstream stages.
3. **Freshness is enforced twice.** Once via the search parameter, once by checking the posting date on our side.
4. **Grounding.** Tailoring may only claim what exists in the owner's proof points.
5. **Raw data is stored before normalization**, so filter or scoring changes can be replayed without re-scraping (reduces rate-limit exposure).

## Boundaries
- Java owns: config, profile, filtering, scoring, ranking, tailoring, output.
- Python owns: only the JobSpy call. Nothing else.
- Open: how they communicate (subprocess or local service). See `08-open-questions.md`.
