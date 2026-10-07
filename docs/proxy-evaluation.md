# Proxy evaluation (Q6)

Question: do we need a proxy for LinkedIn, and does it need infrastructure?

## Verified (JobSpy README, checked 2026-10-07)
- JobSpy takes a `proxies` list in the form `user:pass@host:port`. It round-robins through them.
- README: "LinkedIn is the most restrictive and usually rate limits around the 10th page with one ip. Proxies are a must basically."
- Proxies are not strictly required for basic use. A 429 means the board blocked the IP; the README advice is to wait or change IP.

## Our expected volume
- 2 searches per run, 10 results each (about 1 search page each), plus one detail request per job when descriptions are fetched (up to about 20).
- Total: roughly 25 requests per manual run, far below the roughly 10-page limit per search.

## Infrastructure needed: none
- A proxy is a service you rent. You get a host, port, user and password. There is no server to run and nothing to deploy.
- Code effort is small: one config key and one extra field passed to the sidecar (about 5 lines). Not built, because it is not needed yet.

## Options (general knowledge, not verified in this session; check provider pricing before buying)
| Option | Cost | Effort | Notes |
|---|---|---|---|
| Own home IP (current) | free | none | Fine at our volume. A block affects only your IP, not your LinkedIn account (no login is used) |
| Change IP when blocked (phone hotspot or VPN) | free or low | manual | Good fallback if a 429 appears |
| Datacenter proxy | low, paid | config only | LinkedIn commonly blocks these IP ranges |
| Residential rotating proxy | paid, usually per GB or monthly plan | config only | Works better. Our traffic is tiny, but some plans have monthly minimums |

## Does a proxy change the terms-of-service risk?
No. LinkedIn's terms generally prohibit automated scraping (general knowledge). A proxy only hides the IP. Because no login is used, the account is not involved.

## Recommendation
Do not buy a proxy now. Run by hand from your own IP. The run metrics (`data/runs/`) record every failed search with its error text, so a 429 will be visible. Add a proxy only if blocks become frequent.
