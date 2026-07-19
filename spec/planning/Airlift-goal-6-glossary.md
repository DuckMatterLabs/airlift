---
confidence: proposed
related: [spec/drafts/Airlift-article.md, spec/planning/Airlift-goal-4-mcp.md, spec/planning/Airlift-goal-7-productionalization.md, Airbase]
sources: [spec/drafts/Airlift-article.md, spec/drafts/Airlift-IR.md, spec/analysis/IR-analysis.md, ir-spec/IR-SCHEMA.md, out/ofbiz-tax/ir/]
trajectory: >
  Executes as M8 after M3 (needs two target glossaries to prove federation);
  resolve_term in the Goal 4 MCP consumes it; the Airbase bridge it builds is
  the substrate Goal 7 productionalizes; ends in a Goal6-handoff.md.
---

# Airlift Goal 6 spec — Glossary federation (Milestone M8)

Status: NOT STARTED. Date: 2026-07-19.
Prerequisites: **M3** (a second target glossary — federation with one glossary is
vacuous). M6 (MCP) benefits but is not required; `resolve_term` grows richer when this
lands.
Background: `spec/drafts/Airlift-article.md` (glossary as lingua-franca pillar, term
linter, three-tier truth status), `spec/drafts/Airlift-IR.md` (glossary frontmatter
sample: canonical / aliases / forbidden_or_ambiguous / not / related / owner / status),
`spec/analysis/IR-analysis.md` pitfall 4 (the scaling problem this goal exists to
solve), `ir-spec/IR-SCHEMA.md` (current per-target glossary).

## Goal summary

The glossary is the whitepaper's cornerstone in two directions at once:

* **Inward** — the lingua franca that makes claims compilable: every noun in every claim
  resolves; agents navigate mechanically; colloquial drift is linted out at ingestion.
* **Outward** — the **bridge between Airlift and the org's other knowledge bases**,
  foremost **Airbase** (our per-team specialization of the Team Brain KB product —
  the names are synonyms; it consolidates tribal knowledge, wikis, PM docs, and
  ongoing meetings). Two KBs can only cooperate if they mean the same thing by the same
  words — the glossary is where that agreement is made explicit, versioned, and owned.

Goal 1 built a closed 75-term glossary for one seam. This goal makes the glossary a
**federated, governed, org-scale layer** without losing the property that earned its
keep: closure — every reference resolves, no dead ends.

## The scaling problem, stated honestly (IR-analysis pitfall 4)

75 terms for one tax seam; a whole org is thousands of terms with genuine cross-domain
polysemy ("party", "adjustment", "account" mean different things per team). A flat YAML
namespace will not survive. Neither will central editorial control — Conway is real, and
term ownership must follow team ownership. The design constraints:

1. Closure must survive federation (a claim never references an unresolvable term).
2. Polysemy is represented, not suppressed (two teams' "account" are two terms with an
   explicit disambiguation edge, not a fight over one entry).
3. Governance is per-partition (owner per namespace), with a small shared core.

## Method

### Phase A — namespacing & federation model

* Term IDs gain namespaces: `<partition>.<term>` (partition ≈ target/team/domain;
  `core.*` for the shared spine — money, quantity, time, the cross-domain primitives).
* Claims reference namespaced terms; an **import declaration** per target manifest
  lists which foreign partitions it may reference (closure now checked per-target
  against declared imports — the validator extension is a versioned schema delta via
  the M2 process).
* **Polysemy protocol**: same surface word in two partitions ⇒ both entries carry
  `homonyms:` cross-references and `not:` discriminators. The linter uses these to
  force disambiguation in ambiguous contexts.
* Merge/deprecation/ownership governance: `supersedes`/`superseded_by`, `owner` per
  partition, deprecation grace (a deprecated term still resolves, with a pointer),
  promotion of a partition term to `core.*` as an owned, reviewed act.

### Phase B — the term linter

Deterministic core + LLM assist, in the standard Airlift split:

* **Deterministic**: given text + a partition context — resolve exact matches and
  aliases; flag `forbidden_or_ambiguous` hits; flag unresolved nouns (candidate terms).
* **LLM-assisted (proposal-only, never truth-path)**: for unresolved nouns, draft a
  "new term?" proposal (definition, aliases, `not:`) routed to the partition owner —
  the whitepaper's "term linter opens a ticket" behavior. Silent additions are
  impossible by construction.
* Lint surfaces: claim ingestion (already enforced), pipeline stage outputs, PR
  descriptions (via Goal 2 Ledger / Goal 7 Maestro hooks), and Airbase documents
  (Phase C).

### Phase C — the Airbase bridge

The responsibility split (see Goal 7 spec §split for the full statement): Airlift terms
are **code-anchored facts** (bound to claims/fragments — "this is what the code means by
Tax Exemption, provably"); Airbase concepts are **intent-anchored** (what engineers and
PMs mean when they talk). The bridge:

* **Alias mapping**: Airbase concept ↔ Airlift canonical term, explicit and versioned
  (`airbase_concepts:` on the term, or a separate mapping table — decide at execution).
  Where Airbase has a concept Airlift lacks: that's fine — Airlift is un-opinionated
  and only speaks where code speaks. Where Airlift has a term Airbase lacks: an export
  candidate.
* **Anchor provenance** per term: `anchored: code | intent | both`. A `code`-anchored
  term's definition is *warranted* (claims + tests behind it); an `intent`-anchored
  concept is Airbase's to govern. Both-anchored terms are the bridge's load-bearing
  span — and the place drift between what-code-does and what-people-mean becomes
  *visible* instead of latent.
* **Round-trip**: an Airbase document linted against the federated glossary — every
  noun resolves (to either side) or produces a proposal. This is the operational test
  that the two KBs actually share a language.

## Deliverables

* Schema delta (versioned, via M2 process): namespaced terms, imports, homonyms,
  supersession, `anchored:` provenance.
* Federation of the two existing target glossaries (ofbiz-tax + M3 target), with a
  seeded `core.*` partition.
* `pipeline/lint-terms.py` (deterministic core) + proposal prompt (LLM assist);
  wired into claim/stage validation.
* Airbase mapping format + one worked mapping set; governance doc (ownership, merge,
  deprecation, core-promotion), each rule exercised at least once.

## Exit criteria

1. **Federation proof**: both target glossaries live under one federated namespace;
   all claims still validate (closure preserved); at least one genuine polysemy pair
   is represented with working disambiguation.
2. **Linter proof**: on a document seeded with colloquialisms, forbidden terms, and
   novel nouns — the linter resolves the resolvable, flags the forbidden, and emits
   proposals for the novel; zero silent additions.
3. **Bridge proof**: one real (or realistic) Airbase document round-trips — every noun
   resolves to a canonical term (either anchor) or an open proposal; the mapping table
   carries ≥10 concept↔term links with anchor provenance.
4. Every governance rule (merge, deprecate, promote-to-core, ownership transfer) has
   been executed once, with the audit trail in git.

## Non-goals

* Distilling Airbase's content — Airbase owns intent capture; this goal only makes the
  shared vocabulary real.
* Ontology formalism (subsumption hierarchies, description logic). Flat terms +
  namespaces + explicit edges, per the reflexive discipline: walls must be hit before
  machinery is built (a formal-ontology need would be a recorded falsifier observation
  on AADR-002, not a speculative build).
* Embedding-based term matching on the truth path (same refusal as Goal 4; similarity
  may *suggest* proposals, never resolve references).
