---
confidence: proposed
related: [spec/analysis/socraticode-analysis.md, spec/analysis/IR-analysis.md, spec/drafts/Airlift-article.md]
sources: [spec/planning/MILESTONES.md, spec/analysis/IR-analysis.md, spec/analysis/socraticode-analysis.md, spec/drafts/Airlift-article.md]
trajectory: >
  Executes as M6 after M2; serves M4/M5 outputs when present; verify tool
  becomes ambient in Goal 5; ends in a Goal4-handoff.md.
---

# Airlift Goal 4 spec — Airlift MCP (Milestone M6)

Status: NOT STARTED. Date: 2026-07-19.
Prerequisite: **M2** (schema v1.0 + claim-status promotion tool — tiers must exist before
they can be served). M4/M5 outputs (provenance narratives, architecture artifacts) are
served when present, never required.
Background: `MILESTONES.md` §M6, `spec/analysis/IR-analysis.md` §"Progressive disclosure in an
Airlift MCP" (the layer design — normative for this goal),
`spec/analysis/socraticode-analysis.md` (what to borrow from SocratiCode and what to refuse),
`spec/drafts/Airlift-article.md` §Serving + §Indemnity.

## Goal summary

Make the IR **the cheapest path** for agents: an MCP query plane serving claims,
glossary, traceability, spine status, and provenance to coding agents and harnesses
(Copilot, OpenAI Symphony, Claude Code). Two properties distinguish it from generic
RAG-over-docs:

* **Verified content**: a fetched claim is backed by a green blind test and carries its
  tier; and
* **Closed vocabulary**: every noun in a served claim resolves in the glossary, so agents
  navigate mechanically, with no dead ends and no semantic guessing.

And one property from the whitepaper's indemnity design: the server is a **clerk, not an
oracle** — every answer ships the paper trail (claim ID, status tier, owner, provenance),
never a bare assertion.

## Inputs (fixed contracts)

* `tests/out/<target>/ir/` — the only source of truth. The MCP is a **view**; it holds no
  knowledge of its own and can be restarted/rebuilt from the IR files at any time.
* `ir-spec/IR-SCHEMA.md` v1.0 — response shapes derive from it.
* Claim statuses from the M2 promotion tool (`extracted → pinned → verified`, plus
  tier-3 `inferred` from Goal 2).
* `tests/targets/<name>/harness/` — `harness-contract.md` and the spine runner, for the
  `verify` tool.

## Tool surface (progressive disclosure — the IR-analysis layers, made concrete)

Each layer is a few hundred tokens and hands out IDs that the next layer resolves:

1. `ir_overview(target?)` — manifest level: target, seam, claim counts by
   kind/area/status, unpinned claims, schema version. (~200 tokens.)
2. `list_claims(area?, kind?, status?, term?, fragment?)` — IDs + one-line titles only.
   `DOMAIN.AREA.SLUG` IDs are self-describing; scanning `TAX.EXEMPT.*` titles teaches the
   shape of the logic before a single body is fetched.
3. `get_claim(id)` — the full atom: constrained Gherkin, scenarios, decision table,
   `depends_on`, `outputs_affected`, terms — **plus, always**: status tier, owner,
   provenance pointer, bound tests + last verification result.
4. `resolve_term(term)` / `get_dependencies(id, transitive?)` — lazy glossary and
   dependency-closure expansion. Closed vocabulary guarantees resolution.
5. `trace(claim_id)` / `claims_for(file?, symbol?, fragment?)` — the bidirectional code
   bridge. `claims_for` is the killer query for a coding agent: "I'm editing
   `handlePartyTaxExempt` — what am I about to violate?"
6. `get_harness_contract(target)` — mechanics-only fixture doc (the E1 input set,
   served).
7. `verify(claim_ids?)` — run the bound spine tests, return violated-claim names
   (the E4 experience inside an agent session). Long-running: background start +
   status polling (see SocratiCode borrowings).
8. `get_provenance(claim_id)` — Goal 2 narratives when present, `tier: inferred`
   prominently marked; git-stub data otherwise.

**Tier discipline is structural, not decorative**: every response envelope carries
`{claim_id, status, owner, provenance}`; `inferred` content is visually and structurally
distinguished from `verified`/`pinned` in every tool's output. Blur it once and the
indemnity design is burned (whitepaper §Indemnity).

## Borrowed from SocratiCode (and what is refused)

Per `spec/analysis/socraticode-analysis.md` — SocratiCode has *shipped* the MCP ergonomics this
goal needs; steal the product engineering, refuse the epistemics:

* **Tool-surface ergonomics**: `projectPath`/target defaults to cwd resolution; a
  `status` tool; long operations (`verify`, re-index) run in background and return
  progress on poll — SocratiCode's `codebase_index`/`codebase_status` pattern verbatim.
* **Distribution shape**: installable as an npm/npx MCP server plus a **Claude Code
  plugin bundling skills** — an "airlift-grounding" skill that teaches the agent the
  workflow (*check claims before editing bound code; `claims_for` before refactor;
  `verify` after change*), the exact analog of SocratiCode's search-before-reading skill.
  Agent-instruction blocks for non-plugin hosts (CLAUDE.md / AGENTS.md snippets) ship in
  the README, SocratiCode-style.
* **Project identity**: a committed `.airlift.json` (`{"target": "ofbiz-tax", "ir":
  "tests/out/ofbiz-tax/ir"}`) so any checkout resolves the same IR — SocratiCode's committed
  `projectId` move.
* **Multi-agent read concurrency** is free (IR files are static); `verify` takes a
  cross-process lock (SocratiCode's `proper-lockfile` pattern) since the spine mutates
  the OFBiz working tree.
* **Interim integration, zero code**: register `tests/out/<target>/ir/**/*.yaml` as SocratiCode
  *context artifacts* (`.socraticodecontextartifacts.json`) so teams already running
  SocratiCode surface verified claims through `codebase_context_search` before Airlift
  MCP ships. This is a bridge, not the product.
* **Refused — embedding search as the primary interface.** The closed glossary +
  self-describing IDs give deterministic, dead-end-free navigation, strictly stronger
  than similarity ranking at this vocabulary size. A `search_claims(query)` convenience
  entry point may exist, but layers 1–5 must be fully navigable without it, and its
  results carry the same tier envelopes. Silent retrieval misses are SocratiCode's known
  failure mode; do not import it onto the truth path.
* **Refused — efficiency-only benchmarking.** SocratiCode's headline numbers measure
  context/token savings. This goal's exit measures **task correctness under blindness**;
  report token cost secondarily.

## Deliverables

* MCP server (stdio) over `tests/out/<target>/ir/` with the tool surface above; multi-target.
* Claude Code plugin (skills + agent instructions); README config blocks for Copilot CLI
  and other MCP hosts.
* `.airlift.json` resolution; `verify` lock + background execution.
* Deterministic response-envelope validator (every response schema-checks, every claim
  reference in a response resolves) — same validator discipline as the pipeline.
* E1-style blind-agent exit runner: `tests/exit/run-e6-mcp-blind.sh`.

## Exit criteria

1. **MCP-blind implementation test**: an external coding agent (Copilot+Opus, E1-style
   enforced sandbox — no repo file access, no web; the MCP is its *only* grounding)
   implements a small, deliberate behavior change: a ratified claim delta is served via
   the MCP (e.g., a modified threshold claim), the agent writes the code change + new
   tests from MCP answers alone; the full claim suite plus its new tests run green
   against real code.
2. **Tier discipline holds**: automated check over recorded MCP traffic — every response
   carries claim ID, status tier, owner, provenance; no `inferred` content ever appears
   without its marking.
3. **No dead ends**: a crawler exercises layers 1→5 exhaustively from `ir_overview`;
   every ID/term/fragment handle returned by any tool resolves in the appropriate
   next-layer tool.
4. `verify(claim_ids)` reproduces E4: against the flipped-rule mutation, it returns
   exactly the four TAX.EXEMPT claim names.

## Non-goals

* Serving unverified narration. If it isn't in the IR (or an explicitly-tiered
  provenance narrative), the MCP doesn't say it.
* Write access. Agents propose IR deltas through the normal ratification path (Goal 5's
  compensation loop / PRs), never through MCP mutation tools.
* General code search. That's SocratiCode's product; compose with it, don't rebuild it.
