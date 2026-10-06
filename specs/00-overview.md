# 00. Overview

## Goal
An agent that finds job postings, filters and ranks them against the owner's capabilities, and prepares tailored application material that highlights the owner's relevant strengths. The owner applies manually.

## In scope
1. Collect the owner's capabilities into a structured profile.
2. Collect raw job postings, primarily from LinkedIn.
3. Filter (hard rules) and rank (scoring) the postings.
4. Tailor CV content per job, highlighting matching capabilities.
5. Produce a shortlist and tracking output.

## Out of scope (DECIDED)
- **Auto-applying / submitting applications.** The owner chose "discovery only". The agent finds, filters, ranks and tailors; the human applies.

## Out of scope for the first version (DECIDED)
- Sources other than LinkedIn. Indeed and Glassdoor come in a later phase, via API/MCP only if they are easy to connect.

## Why freshness matters
The owner wants only fresh postings (default: max 7 days old, configurable), because older postings are saturated with applications and early applicants have better odds.
