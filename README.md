# autonomous-job-finder-agent
Subject: An intelligent, automated software application designed to streamline the job hunt by scraping postings, filtering by custom criteria, and dynamically adapting to different application workflows.

## Run (discovery only: the agent never applies to jobs)
Prerequisites: `sdk env` (Java 25, Maven 3.9.10), Python 3.10+ venv in `sidecar/` (`pip install -r sidecar/requirements.txt`), `private/profile.json` (gitignored), and these environment variables:
`OLLAMA_API_KEY` and `OLLAMA_MODEL` (an Ollama Cloud model).

```
cd app
mvn test                                                     # unit tests, no network
mvn spring-boot:run -Dspring-boot.run.arguments=run          # full run: LinkedIn, filter, score, CVs, shortlist
```
Output goes to `data/` (gitignored): `shortlist_<date>.md`, `cv/*.pdf`, `raw/`, `runs/run_*.json` (searches, LinkedIn result quality, funnel, token usage).
Specs are in `specs/`, the audit log in `docs/agent-log.md`.
