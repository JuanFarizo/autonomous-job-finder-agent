# autonomous-job-finder-agent
Subject: An intelligent, automated software application designed to streamline the job hunt by scraping postings, filtering by custom criteria, and dynamically adapting to different application workflows.

## How to use it
The agent finds jobs, ranks them against your real profile and prepares a tailored CV for the best ones. You review the results and apply by hand. It never applies to anything.

1. Complete the one-time setup in "How to test it, and what you must provide" below.
2. Run `./scripts/run.sh`. A run takes about a minute and makes two LinkedIn searches and one model call per job.
3. Open `data/shortlist_<date>.md`. Each entry has a score from 1 to 5, a short reason, the location, the posting date and the LinkedIn URL.
4. Open the matching PDF in `data/cv/`. The file name is `linkedin_li-<id>.pdf`, where `<id>` is the number at the end of the job URL. Only the top 5 new jobs of each run get a PDF.
5. Apply on LinkedIn yourself.
6. Run it again whenever you want. Jobs already shown are not shown again. To start from zero, delete `data/tracker.json` and `data/seen_jobs.json`.

To change what it looks for, edit `config/config.json` (search wording, seniority, languages, location rules, `min_score`). To change how jobs are judged, edit `private/profile.json` (your skills, proof points and job preferences).

## Run (discovery only: the agent never applies to jobs)
Run it by hand. There is no scheduler.
```
./scripts/run.sh                                            # works from any terminal: loads sdkman Java/Maven and the keys
cd app && mvn test                                          # unit tests, no network, no keys needed
```
Exported variables exist only in the terminal where you typed them. Put them once in `private/.env` (gitignored) so `scripts/run.sh` always finds them. Use plain `KEY=value` lines, without `export`:
```
OLLAMA_API_KEY=...
OLLAMA_MODEL=gemma4:31b
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

## Architecture
A Java application runs the whole pipeline. Python is used only to call JobSpy. The language model is used only to score jobs. Everything else is deterministic code.

```
config/config.json + private/profile.json
              |
              v
   [Collector]  builds 2 search queries, calls the sidecar, stores raw results in data/raw/
              |        JSON on stdin/stdout
              |        +--> sidecar/run_jobspy.py  --> JobSpy 1.2.0 --> LinkedIn (no login, own IP)
              v
   [Hard filters]  deterministic: age, location, seniority, excluded words, lead/principal/staff
              |
              v
   [Skip tracked]  jobs already in data/tracker.json are dropped
              |
              v
   [Scorer]  one call per job to Ollama Cloud (Spring AI); structured answer with a score,
              |   a reason and evidence ids; the answer is rejected if an id is not in your profile
              v
   [Ranker]  keeps jobs with score >= min_score, sorted by score, then freshness
              |
              v
   [Tracker + Tailorer]  records the job; for the top 5 new jobs reorders your real skills,
              |   proof points and bullets by overlap with the job text; renders HTML to PDF
              v
   data/shortlist_<date>.md, data/cv/*.pdf, data/runs/run_*.json
```

### Components and external dependencies
| Part | What it does | Dependency | Cost |
|---|---|---|---|
| Orchestrator | Pipeline, filters, ranking, tracking, output | Java 25, Spring Boot 4.0.8, Maven 3.9.10 (pinned in `.sdkmanrc`) | Free |
| Collector sidecar | Scrapes LinkedIn job postings | JobSpy 1.2.0 (MIT) in a Python 3.12 venv, called as a subprocess | Free |
| Scorer | Judges the fit between a job and your profile | Spring AI 2.0.1 with Ollama Cloud (`https://ollama.com`), free-tier model set in `OLLAMA_MODEL` | Free tier |
| Scoring rubric | Dimensions and score bands | Trimmed port of the career-ops rubric (MIT, attribution in `app/src/main/resources/prompts/`) | Free |
| PDF CV | Renders the tailored CV | openhtmltopdf 1.1.93 (LGPL-2.1+) and the HTML template `cv-template.html` | Free |
| Storage | Seen jobs, tracker, raw results, metrics | Plain JSON files in `data/` (gitignored) | Free |

### Design rules
- **Discovery only.** There is no code that submits an application.
- **Filters before the model.** Only jobs that pass the deterministic filters are scored, which limits model calls.
- **Grounding.** Every proof point has a source (`cv` or `owner-stated:<date>`). The scorer rejects any evidence id that is not in your profile, and the tailorer only reorders text that already exists in it. Nothing is invented.
- **Raw data first.** Search results are saved in `data/raw/` before any processing.
- **Private data stays local.** `private/` and `data/` are gitignored. Keys come from environment variables or `private/.env`.
- **One interface per source.** LinkedIn is the only source today. Others can be added behind the same `JobSource` interface.

The full decisions and their reasons are in `specs/` (start with `specs/README.md` and `specs/02-decisions-log.md`).
