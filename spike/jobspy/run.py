import json
from jobspy import scrape_jobs
df = scrape_jobs(site_name=["linkedin"], search_term="Senior Java Developer", location="Argentina",
                 is_remote=True, results_wanted=15, hours_old=168, linkedin_fetch_description=False, verbose=2)
print("ROWS", len(df)); print("COLUMNS", list(df.columns))
df.to_json("sample.json", orient="records", indent=2, date_format="iso")
print(df[["title","company","location","date_posted","is_remote","job_type"]].to_string())
