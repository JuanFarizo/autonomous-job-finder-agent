# Autonomous Job Finder Agent: Specification

This folder is the starting point for the project. It records every decision taken during planning, the evidence behind them, and what is still open.

## How to read this spec

| File | Purpose |
|---|---|
| `00-overview.md` | Goal, scope, non-goals |
| `01-constraints.md` | Hard constraints that shape every other decision |
| `02-decisions-log.md` | Every decision with its status and origin |
| `03-technology-stack.md` | Chosen and candidate technologies, with verification status |
| `04-architecture.md` | Pipeline, data flow, module boundaries |
| `05-phases.md` | Implementation phases in dependency order, with exit criteria |
| `06-configuration.md` | Draft of the user-facing configuration |
| `07-risks.md` | Known risks and mitigations |
| `08-open-questions.md` | Decisions still to be made |
| `research/validated-projects.md` | Existing projects that passed validation |
| `research/discarded-projects.md` | Projects rejected and why |

## Status legend

- **DECIDED**: explicitly chosen by the project owner.
- **PROPOSED**: suggested during planning, not yet explicitly confirmed.
- **PENDING**: needs research or a spike before it can be decided.
- **VERIFIED**: evidence checked on the project's repository or documentation. This does NOT mean it was executed. Execution is part of Phase 0.

## Ground rules from the owner

- Do not assume. If in doubt, ask first.
- Projects that cannot be validated are discarded, not used.
- Start simple (LinkedIn only), extend later.

Planning date: 2026-10-06.
