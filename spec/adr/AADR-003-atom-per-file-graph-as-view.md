---
confidence: analyzed
related: [spec/adr/AADR-002-closed-glossary.md, spec/planning/Airlift-goal-4-mcp.md, Code-Digital-Twin]
sources: [spec/analysis/Airlift-reflexive.md, spec/analysis/IR-analysis.md, spec/drafts/Airlift-article.md]
trajectory: >
  Principle frozen at M2; directory sharding/namespacing expected as a minor
  delta when scale forces it; the graph materializes for Goal 4's MCP index
  using CDT's typed edge vocabulary as seed — always as a rebuildable view.
---

# AADR-003 — One atom per file; flat YAML; the graph is a view

**Tag:** principle load-bearing; current flatness incidental. **Status:** active.
**Origin:** reflexive D3.

## Decision

Each claim atom lives in its own flat-YAML file. Any graph or index over the IR is a
rebuildable projection — never the source of truth. (The whitepaper's two-layer
store: MD/YAML is the log, the graph is the projection; event sourcing.)

## Alternatives considered

* **Database-first store** — rejected: puts a mutable store on the truth path and
  loses git-diffable ratification units (KB-as-code requires PR-reviewable atoms).
* **Monolithic YAML per area** — rejected implicitly by the progressive-disclosure
  requirement ("one atom per file" is literally in the schema).

## Evidence

Per-claim repair (E1's loop edited individual claim files) and per-claim spine
reporting (`report-claims.py` maps test results to claim IDs) both exploited file
granularity. IR-analysis: "the IR is already a graph in denial" — claims→terms,
claims→claims (`depends_on`), claims→fragments, claims→tests — and materializing it
as a *view* buys impact queries, coverage topology, and the Goal 2 Ledger edges.

## Falsifier

Navigation/coverage queries become impractical without the materialized graph
becoming the truth. This must never happen — if it does, the two-layer store thesis
failed and the whitepaper's §I needs revisiting, not just the schema. (The graph as
view keeps the property that every node is backed by a green blind test.)

Current *flatness* is separately falsifiable and expected to fall: directory
sharding/namespacing will be forced by scale — that is a delta, not a break.

## Falsifier observations

* 2026-07-19 — none tripped. Borrowing recorded (IR-analysis, CDT section): the
  graph-as-view should adopt Code Digital Twin's typed edge vocabulary
  (`justified-by`, `constrained-by`, `operationalized-by`, …) and Twin-RAG's
  graph-first retrieval as the MCP index algorithm — both compatible with, and
  strengthening, the view-only discipline.
