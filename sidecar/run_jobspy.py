"""JobSpy sidecar. Reads one JSON query on stdin, prints a JSON array of raw jobs on stdout.
Errors go to stderr with a non-zero exit code. Nothing else happens here (see specs/04-architecture.md)."""
import json
import sys

from jobspy import scrape_jobs


def main() -> int:
    q = json.load(sys.stdin)
    try:
        df = scrape_jobs(
            site_name=["linkedin"],
            search_term=q["search_term"],
            location=q.get("location"),
            is_remote=q.get("is_remote", False),
            job_type=q.get("job_type"),
            results_wanted=q.get("results_wanted", 10),
            hours_old=q["hours_old"],
            linkedin_fetch_description=q.get("fetch_description", False),
        )
    except Exception as e:  # any scraper failure: report and exit, Java decides what to do
        print(f"jobspy error: {type(e).__name__}: {e}", file=sys.stderr)
        return 2
    sys.stdout.write(df.to_json(orient="records", date_format="iso"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
