# 06. Configuration (draft)

Only `max_age_days` is DECIDED. All other keys are a draft to be confirmed in Phase 1.

| Key | Meaning | Default | Status |
|---|---|---|---|
| `max_age_days` | Maximum age of a posting | 7 | DECIDED (configurable) |
| `sources` | Enabled sources | `[linkedin]` | DECIDED (LinkedIn first) |
| `target_roles` | Titles/keywords to search | `["Java Web Developer"]` | DECIDED role (owner: Java Senior Web Developer); exact title list PENDING |
| `location` | Rules object, see D21 | residence AR | DECIDED (onsite in Argentina still null) |
| `seniority` | Accepted levels | semi-senior, senior | DECIDED (D20) |
| `languages` | Accepted posting languages | es, en | DECIDED (D20) |
| `exclude_keywords` | Postings to drop | none | PROPOSED |
| `min_score` | Score threshold to enter the shortlist | none | PROPOSED |
| `results_per_search` | Cap per search | none | PROPOSED |

Open: the owner has not yet specified target roles, locations or seniority. Do not assume them.
