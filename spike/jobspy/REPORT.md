# T1: JobSpy LinkedIn run (2026-10-06, owner's own IP, no proxy)
Call: python-jobspy 1.2.0, Python 3.12.11 venv. site=linkedin, term "Senior Java Developer", location "Argentina", is_remote=True, results_wanted=15, hours_old=168, linkedin_fetch_description=False. One run only. Script: run.py. Raw output: sample.json (gitignored).
- Result: 15 rows, 2 search pages, about 8 seconds. No error, no rate limit, no block.
- Dates: all within window (2026-09-29 to 2026-10-06). job_url filled for 15/15.
- Empty fields: description 0/15 (expected, fetch off), job_type 0/15, job_level 0/15.
- is_remote was False for 15/15 although is_remote=True was passed. Remote filter is NOT reliable here; our own filter must decide remote (needs description or location text).
- Contractor filter: JobSpy has job_type param but job_type column came back empty; not tested. Open.
- Several results are generic Argentina onsite or hybrid roles (Globant, EPAM, etc.).
