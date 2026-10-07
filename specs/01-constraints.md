# 01. Constraints

| ID | Constraint | Status | Origin |
|---|---|---|---|
| C1 | No automatic submission of applications | DECIDED | Owner |
| C2 | LinkedIn is the primary and only initial source | DECIDED | Owner |
| C3 | Additional sources (Indeed, Glassdoor) only later, and only if an API/MCP is easy to connect | DECIDED | Owner |
| C4 | Only fresh postings: `max_age_days` default 7, configurable | DECIDED | Owner |
| C5 | Existing projects are reused only if validated; unvalidated ones are discarded | DECIDED | Owner |
| C6 | Language choice by best fit; Java is the tiebreaker if quality is equal | DECIDED | Owner |
| C7 | No assumptions: open questions go to the owner | DECIDED | Owner |
| C8 | Tailored CV content must be grounded in the owner's real, provable experience (no invented claims) | PROPOSED | Derived from career-ops design; to be confirmed |
| C9 | Prefer collection methods that do not use the owner's personal LinkedIn session | PROPOSED | Risk-driven; to be confirmed |

## Constraint: no cost
The owner accepts no paid services (D33). Ollama Cloud runs on the free tier only. If a free tier limit is hit, the run fails safely and the owner decides; nothing is bought automatically.
