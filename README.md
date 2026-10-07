# autonomous-job-finder-agent
Subject: An intelligent, automated software application designed to streamline the job hunt by scraping postings, filtering by custom criteria, and dynamically adapting to different application workflows.

## Run (discovery only: the agent never applies to jobs)
Run it by hand from `app/`. There is no scheduler.
```
cd app
mvn test                                                     # unit tests, no network, no keys needed
mvn spring-boot:run -Dspring-boot.run.arguments=run          # full run: LinkedIn, filter, score, CV PDFs, shortlist
```
Output goes to `data/` (gitignored): `shortlist_<date>.md`, `cv/*.pdf`, `raw/`, `runs/run_*.json` (searches, LinkedIn result quality, funnel, token usage).

## How to test it, and what you must provide
1. **Tools (once):** `sdk env` (Java 25.0.4 and Maven 3.9.10 from `.sdkmanrc`), and Python 3.10+.
2. **Sidecar (once):** `cd sidecar && python -m venv .venv && .venv/bin/pip install -r requirements.txt`.
3. **Your profile (you provide):** `private/profile.json`, gitignored. It holds your real skills, experience and proof points. Every proof point needs a source (`cv` or `owner-stated:<date>`), or loading fails. The CV PDF (`private/cv.pdf`) is only the source to build it from.
4. **Ollama (you provide):**
   - `export OLLAMA_API_KEY=...` (create it at ollama.com/settings/keys).
   - `export OLLAMA_MODEL=...` (a free-tier cloud model name from ollama.com/search?c=cloud). The run stops with a clear message if it is missing.
5. **Config (already set):** `config/config.json` has seniority, languages, location rules and the search query. `min_score` is empty, so a placeholder of 3.5 is used and logged until you choose a value.
6. **LinkedIn:** no account, login or key is needed. It runs from your own IP with no proxy (see `docs/proxy-evaluation.md`).
7. **Check:** after a run, open `data/shortlist_<date>.md`, the PDFs in `data/cv/`, and `data/runs/run_*.json` for failed searches and token totals.

Nothing else is required: no database, server, proxy or Anthropic key.
