---
confidence: analyzed
related: [spec/adr/AADR-001-constrained-gherkin.md, spec/planning/Airlift-goal-6-glossary.md, spec/planning/Airlift-goal-4-mcp.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/analysis/IR-analysis.md, tests/out/ofbiz-tax/ir/]
trajectory: >
  Frozen into schema v1.0 at M2; governance falsifier measured at M3
  (second-target glossary friction); federation answer is Goal 6.
---

# AADR-002 — Closed glossary; every claim noun must resolve

**Tag:** load-bearing. **Status:** active. **Origin:** reflexive D2.

## Decision

The glossary is a closed vocabulary: every noun in every claim must resolve to a
glossary entry. Glossary entries carry disambiguation (`not:` fields).

## Alternatives considered

* **Open vocabulary + embedding similarity** — rejected: similarity-based resolution
  is exactly the "agent bridges the gap by guessing" failure the whitepaper diagnoses
  in RAG-over-docs. (Same refusal recorded against SocratiCode: no embedding search on
  the truth path.)

Chosen for lossless semantics (the "Lingua franca" pillar) and mechanical
navigability — no dead ends, the property the MCP progressive-disclosure design
(IR-analysis) now leans on: every noun an agent meets in a claim body is resolvable.

## Evidence

75 terms for the tax seam; blind agents used glossary terms correctly 35/35; the
`not:` fields carried real disambiguation weight ("tax-exemption is *not* a zero tax
rate" — the E4 flip was named via exactly these claims).

## Falsifier

Glossary governance cost grows super-linearly at the 2nd/3rd target, or cross-domain
polysemy ("party," "adjustment") proves unmanageable in a flat namespace. IR-analysis
pitfall 4 states the known cost: a whole ERP is thousands of terms, and the current
flat YAML won't survive that. The falsifier reopens the *flat single namespace*, not
the closed-world principle — Goal 6 (federation, namespacing, term linter) is the
sanctioned evolution path.

## Falsifier observations

* 2026-07-19 — none tripped; single-target data only. IR-analysis pitfall 4 recorded
  as the standing concern to measure at M3.
