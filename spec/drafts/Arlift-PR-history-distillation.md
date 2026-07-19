# Airlift PR history distillation

> Prompt: how do I take advantage of PR history. I think the code evolution, coupled even with a minimal PR description or corresponding workitem ticket (associated with User Story, Epic, Feature branch) is better than any documentation, especially if we can correlate work across multiple repos to de-Conway features. I want you to propose a way to ingest the PR history, and this has to happen only once, so this can involve some heavy lifting. Maybe we can extract all code deltas into a database, and consult them on-demand, or we can try to extract knowledge once and for all.
## Discussion

The motivation is that biz functionality is developed by engineers iteratively, and each iteration normally presents a functioning state.
By going bak in time in git, it becomes clear how various code fragments relate to each other.
Consider it as multilevel blame. Git’s native blame only sees cumulative projection of commits, your map should deconstruct it into layers.

### Stable IDs
The important concept of this point is what to consider a stable ID to reference a fragment of code as.
Files and methods can be renamed or deleted but I think it would be the only possibility.

### What to extract?
"extract knowledge once and for all" is a trap — because extraction without a question is unbounded. 
Most PR history is noise (version bumps, typo fixes, formatting churn), and you can't know which 5% is signal until a claim asks. 
Bulk-summarizing 10 years of PRs produces a mountain of unratified prose nobody trusts — the demo-theater version of archaeology. 
The correct split is: index everything once (cheap, deterministic), mine on-demand per claim (targeted, ratifiable). Index eagerly, extract lazily.
Git already stores every delta; what's missing is the join. 
Build the Ledger once — PRs joined to tickets joined to Epics, across repos — then mine it lazily, claim-by-claim, hotspots first. History is deposed as a witness, not transcribed as scripture: it testifies, and a human ratifies.

## Pipeline
### Phase 1 — the Ledger (one-time, deterministic, no LLM).
Walk every repo's history and build a structured index — pointers only, never diff contents:
```text
PR(id, repo, title, description, author, merged_at)
  ├─ commits → files/symbols touched
  ├─ work_item(id) → parent Story → Feature → Epic
  └─ reviewers, linked incidents
```
Small (metadata for ~100k PRs fits in SQLite), rebuildable, and it immediately materializes the Feature Ledger: Epic-4711 → 23 PRs across 5 repos, 14 months — the cross-repo feature reconstruction you wanted, before any AI touches anything. This alone de-Conways the history even before the KB de-Conways the knowledge.
> TODO: compare ledger to the existing PR Kusto database

### Phase 2 — claim-driven mining (on-demand, LLM, human-ratified). 
When a claim needs provenance or WHY, run the archaeology backwards from the claim:
1. @Claim("RULE-0107") binding → git blame the bound code → the commits that shaped it.
2. Ledger joins commits → PRs → tickets → sibling PRs on the same Epic (the cross-repo context).
3. LLM synthesizes over that bounded bundle — diffs + PR descriptions + ticket text: "the 5-minute dedup window was introduced in PR #2381 under Epic-4711 after incident INC-889; widened in #3105 with rationale X."
4. Output lands as an inferred claim (your tier-3) awaiting human ratification — mined history explains what changed and when with certainty, but why is reconstruction, and PR descriptions are famously thin ("fix bug"). Archaeology yields evidence, not verdicts; a human promotes it.
### Phase 3 — prioritize by heat, not completeness. 
Don't mine uniformly. The Ledger gives you churn-per-symbol and incident-correlation for free — mine the hotspots first (high churn + incident-linked + claim-bound), because that's where history has the most to confess. Cold code's history can stay unmined forever; the Ledger holds the pointers if anyone ever asks.


## Existing art
Practical takeaways:
* Don't build Phase 1 from scratch. Code Maat (https://github.com/adamtornhill/code-maat) / CodeScene-style analysis gives you churn, coupling, and hotspot detection off the shelf; your build effort is the join to AzDO work items and the claim-driven miner — the parts that don't exist.
* Use the ideas, not necessarily the implementation:
  * Reuse or reimplement its metrics over Ledger tables.
  * Validate your churn and coupling calculations against it.
  * Do not force it to become the provenance database it was never designed to be.
  * Steal the vocabulary. "Hotspots," "temporal coupling," "behavioral code analysis" are established, citable terms with a book behind them (Your Code as a Crime Scene, and its sequel Software Design X-Rays). Saying "temporal coupling across repos reveals de-Conway candidates" borrows a decade of legitimacy that "3D search of code" would have to earn alone.
* The Lore paper's (Lore: Repurposing Git Commit Messages as a Structured Knowledge Protocol for AI Coding Agents) framing is a gift for this doc: independent researchers describe the Airlift-shaped architecture as the serious-enterprise solution (their example: the Linux kernel) and only object that most projects can't afford it. Engage can. "The literature considers this the right answer for systems at our scale" is a citation, not a claim.
  What genuinely doesn't exist as a product. The full loop: claim-driven archaeology (mine backwards from a KB claim, not forward from history), joined across repos via the work-item hierarchy, synthesized by LLM into inferred claims, human-ratified into a verified KB with provenance tiers. CodeScene stops at analytics dashboards — it tells humans where the hotspots are; it doesn't feed a source-of-truth system. The MSR literature has the techniques but not the ratification-and-liability layer (your moral-crumple-zone answer). The AI tools have memory but not the test-compilable claim stack. The pieces all exist; the assembly is yours.

## References:
  There's an entire academic field: Mining Software Repositories (MSR), with its own conference running since 2004. Your "3D search" instinct rediscovered its founding premise: version history is behavioral data richer than any documentation. The good news is the specific combination you've designed still isn't shipped anywhere — and the prior art de-risks your pipeline rather than obsoleting it. The landscape in three tiers:
  Tier 1 — the direct ancestor: CodeScene (Adam Tornhill). The commercial embodiment of the Crime Scene thesis I cited earlier, and the closest thing to your Ledger. It mines git history for hotspots (churn × complexity), temporal coupling (files that change together — including across repositories, which is precisely your de-Conway signal: two repos that co-change under the same tickets are one feature wearing Conway's disguise), knowledge maps (who knows what code), and change coupling trends. Its open-source ancestor Code Maat does the core analyses from a git log. If you want the Phase-1 heavy lift mostly off the shelf, this is where to look first — temporal coupling across repos is literally a product feature, not a research idea.
  Tier 2 — the AI-era convergence (last ~12 months), proving you're on the current frontier. The research community is converging on your exact framing right now: one recent paper proposes enriching an artifact every project already has (commit messages) using a mechanism git already supports (trailers), queryable through a tool every agent already knows how to use — and notably, it positions itself as the lightweight alternative to a multi-layered infrastructure involving knowledge graphs, bidirectional traceable links between code artifacts and conceptual models, and continuous automated extraction pipelines — which is, almost verbatim, Airlift. So your architecture already exists in the literature as the "heavyweight enterprise" reference point that lighter tools define themselves against. There's also agentic work on temporal graphs over history: the SZZ algorithm family has traced bug-inducing commits via blame for two decades, and the SZZ algorithm, recognized with the 2026 ACM SIGSOFT Impact Paper Award, and its variants trace deleted (and modified) code in a bug-fixing commit via git blame to identify commits that last modified those lines — your "blame → commits → PRs → tickets" walk is SZZ's mechanic pointed at knowledge instead of bugs. Plus git-native agent-memory tools are appearing, like Kage, described as git-native memory for coding agents that stores decisions and fixes as repo files and verifies them against the codebase, withholding stale knowledge — staleness-aware verification, a miniature of your spine.
  Code Digital Twin: A Knowledge Infrastructure for AI-Assisted Complex Software Development: context engineering faces a fundamental barrier: the required context is scattered across artifacts and entangled across time, beyond the capacity of LLMs to reliably capture, prioritize, and fuse evidence into correct and trustworthy decisions, even as context windows grow. To bridge this gap, we propose the Code Digital Twin, a persistent and evolving knowledge infrastructure built on the codebase. In complex, long-lived systems, code is the endpoint of evolving requirements, constraints, and trade-offs, and the underlying tacit knowledge is often distributed across modules, documents, commits, issues, and discussions. Consequently, code-centric retrieval and context engineering can (partially) recover WHAT the code does today, but they often miss how it evolved and why it took its current form.
  The Future of AI-Driven Software Engineering
##  Requirements
  It should answer questions:
* "which features changed that piece of code in this file?",
* "when this particular functionality was first implemented and bound to which feature?",
* "which repos this feature touched?",
* "how behavior was changed by this feature?"

When codebase distillation represents full behavioral specification, the PR history acts as supporting data to:
* generate that said behavioral specification
* enable consistent product evolution over time. In other words, helps generate future specs, and avoid past pitfalls.
