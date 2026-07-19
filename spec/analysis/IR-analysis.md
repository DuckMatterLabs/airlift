---
confidence: analyzed
related: [spec/analysis/socraticode-analysis.md, spec/analysis/Airlift-reflexive.md, spec/planning/Airlift-goal-4-mcp.md, Understand-Anything]
sources: [spec/planning/Goal1-handoff.md, out/ofbiz-tax/RUN-REPORT.md, ir-spec/IR-SCHEMA.md, out/ofbiz-tax/ir/, /Volumes/Dancer/Develop/AIRLIFT/Understand-Anything]
trajectory: >
  Pitfalls feed reflexive risks R1-R5; MCP section is normative for Goal 4;
  UA borrowings feed Goals 3 and 5. Revisit after M3 evidence lands.
---

# Airlift IR — critical analysis and comparison with Understand-Anything

Date: 2026-07-19. Sources: `spec/planning/Goal1-handoff.md`, `out/ofbiz-tax/RUN-REPORT.md`, `ir-spec/IR-SCHEMA.md`,
the produced IR in `out/ofbiz-tax/ir/`, and the Understand-Anything repo
(clone at `/Volumes/Dancer/Develop/AIRLIFT/Understand-Anything`,
upstream: <https://github.com/Egonex-AI/Understand-Anything>).

## What Airlift actually is

Airlift distills a codebase seam into a **verifiable behavioral contract**: ~34 claim atoms in
constrained Gherkin, a closed glossary (claims may only use glossary nouns), a domain model with
fixture vocabulary, and traceability back to source fragments. The defining move is the
**sufficiency test being operational, not rhetorical**: an agent with no access to the source must
generate tests from the IR alone, and those tests must go green against real code (E1), red against
mutations (E2), stay green through refactors (E3), and *name* the violated claims when behavior
changes (E4). All four passed on OFBiz tax.

## Benefits

**1. The blind-testgen loop is a falsifiable quality gate for documentation.** Every other
"understand the codebase" artifact — wikis, summaries, knowledge graphs — has no way to know when
it's wrong or incomplete. Airlift's did: the first blind trial went 30/35, and the 5 reds decomposed
cleanly into 2 harness bugs and 3 *IR ambiguities* (e.g., a blind reader could construct a
county-only address for containment). The fix went into the claims, never the tests, and a fresh
agent reran. That's the scientific method applied to docs: the IR earns its correctness rather than
asserting it.

**2. Architecture-independence is enforced, not aspired to.** Claims forbid implementation
identifiers; the only architecture the blind generator sees is the harness contract. E3 proved the
payoff: a 675-line refactor extracting 10 methods, and all 35 blind tests stayed green. The claims
survive the code they were extracted from — which is precisely what makes this a
*migration/rewrite* asset, not just documentation.

**3. The verification spine turns test failures into semantic diagnoses.** `@AirliftClaim` binding
means a red suite reports "TAX.EXEMPT.ZEROING violated," not "testCase47 failed." E2's per-mutation
results (M2 → exactly `THRESHOLD-MIN-ITEM-PRICE`) show real discriminating power — mutations mapped
to the *right* claims, often exactly one. This is the seed of an anti-drift system: claims as
monitored invariants with statuses (`extracted → pinned → verified`).

**4. Honest epistemic bookkeeping.** `observability: unobservable-via-contract`,
`unpinned_claims`, `confidence`, the `not:` field in glossary entries ("tax-exemption is *not* a
zero tax rate") — the schema records what it doesn't know. Most extraction pipelines silently paper
over exactly these gaps.

**5. Deterministic validators + repair loops make LLM output an engineering material.** Each stage
is gated by `validate.py`, failures feed back verbatim, ≤3 attempts. The LLM proposes; determinism
disposes.

## Pitfalls

**1. Cost scales per-seam, and the seam was hand-picked.** Goal 1 covered one 772-line class with a
hand-authored seam brief, hand-built fixture DSL, and hand-planted mutations. The harness
(`TaxFixture`, contract doc, spine reporter) is the expensive, expert part — and it's exactly the
part that isn't generated. OFBiz has hundreds of such seams. Until harness construction is itself
distilled, Airlift is a *precision instrument*, not a codebase-scale tool. The "second target"
thread in the handoff is the right stress test but doesn't answer the amortization question.

**2. The blind test passing proves sufficiency, not completeness.** 35 green tests show the IR is
*unambiguous enough to reproduce the behaviors it names*. They don't show the 34 claims are *all*
the behaviors. The catalog stage found 30 behaviors — but a behavior the map stage never fragmented
is invisible to every downstream check. E2's mutations were planted where claims already existed; a
mutation in un-claimed logic would sail through. The honest-gap machinery (`unpinned_claims`)
tracks known unknowns, not unknown unknowns. Coverage of the *claim set itself* is the weakest
link, and mutation testing over the whole seam (not 7 curated bugs) would be the honest measure.

**3. Bug-compatibility is a feature and a trap.** The reference-equality `!= BigDecimal.ZERO` quirk
is deliberately preserved ("bug-compatible behavior is the requirement"). Right for migration
fidelity — but the IR now enshrines a bug as a claim with no marking distinguishing "intended rule"
from "accident we're preserving." A `fidelity: bug-compatible` flag would keep the distillate from
laundering defects into requirements. The `notes:` field can hold this, but it's not queryable.

**4. Closed-world glossary friction at scale.** "Every noun must resolve to the glossary" is what
makes claims compilable — and it's also a bottleneck. 75 terms for one tax seam; a whole ERP is
thousands of terms with genuine cross-domain polysemy ("party," "adjustment"). Glossary governance
(merging, namespacing, drift) is an unsolved scaling problem the current flat YAML won't survive.

**5. The double-LLM confound.** Opus extracted the IR and Opus (fresh context) generated the blind
tests. Shared model priors about "how tax systems work" could let a test pass despite an ambiguous
claim — the two ends fill the same gap the same way. The sandbox blocks *information* leakage but
not *prior* leakage. Cross-model blind generation (have a different model family generate E1) would
strengthen the claim of IR sufficiency considerably, and is cheap to run.

**6. Claims are flat files; relationships are thin.** `depends_on` exists but is barely exploited.
There's no way to ask "which claims transitively depend on jurisdiction resolution?" or "which
glossary terms are load-bearing for exemption?" without grepping YAML. Which leads directly to:

## Future optimizations

### Knowledge graph over the IR (the natural next structure)

The IR is already a graph in denial: claims → terms (`terms:`), claims → claims (`depends_on`),
claims → fragments (`traceability.yaml`), fragments → files/symbols, claims → tests (spine),
claims → commits/PRs (provenance stubs), claims → patterns/ADRs. Materializing it as an actual
graph buys:

- **Impact queries**: "PR touches `F-TAS-013` → which claims → which tests → which downstream
  claims via `depends_on`?" That's the anti-drift spine as a graph traversal instead of a grep.
- **Coverage topology**: unpinned claims, orphan terms, fragments with no claims (candidate missing
  behaviors — a partial answer to pitfall 2).
- **Goal 2 lands naturally**: PR-history distillation *is* edge creation
  (claim → PR → ticket → discussion). The Ledger the handoff describes is a temporal edge index.
- **Cross-target reasoning** once a second target exists: shared glossary concepts, pattern reuse,
  "does Fineract have a claim isomorphic to TAX.EXEMPT.ZEROING?"

The key discipline to keep: the graph should be a *view over* the verified claim files, never the
source of truth — otherwise you lose the property that every node is backed by a green blind test.

### Other high-value optimizations (in rough order)

1. **Full-seam mutation testing as the completeness metric** — replace 7 planted bugs with
   systematic mutants; percent-caught becomes the IR's coverage score.
2. **Claim-status promotion tool** (handoff thread 4) — small, deterministic, and turns the IR into
   a live dashboard of behavioral health.
3. **Harness scaffolding generation** — distill the fixture-DSL-building itself into a pipeline
   stage, since that's the per-target cost center.
4. **Cross-model E1** to kill the prior-leakage confound.

## Progressive disclosure in an Airlift MCP

The IR was *designed* for this — "one atom per file (progressive disclosure)" is literally in the
schema — and it maps onto MCP cleanly as a layered zoom, each layer a few hundred tokens, each with
handles into the next:

1. **`ir_overview`** → manifest-level: target, seam, claim count by kind/area/status, unpinned
   claims. (~200 tokens; the agent decides whether this seam is even relevant.)
2. **`list_claims(area?, kind?, status?, term?)`** → IDs + one-line titles only. The
   `DOMAIN.AREA.SLUG` scheme means IDs are self-describing — an agent scanning `TAX.EXEMPT.*`
   titles already learns the shape of the exemption logic without a single body.
3. **`get_claim(id)`** → the full atom: Gherkin, scenarios, decision table, `depends_on`,
   `outputs_affected`. Constrained Gherkin pays off here — the body is dense and unambiguous, no
   prose to summarize.
4. **`resolve_term(term)`** / **`get_dependencies(id, transitive?)`** → lazy glossary and
   dependency-closure expansion. The closed vocabulary guarantees every noun the agent meets in
   layer 3 is resolvable in layer 4 — no dead ends.
5. **`trace(claim_id)` / `claims_for(file, symbol)`** → drop to code: fragments, files, symbols,
   provenance, bound tests + last verification status. This is the bidirectional bridge — "I'm
   editing `handlePartyTaxExempt`, what am I about to violate?" is the killer MCP query for a
   coding agent.
6. **`verify(claim_ids)`** (the ambitious layer) → run the bound spine tests and return
   violated-claim names, closing the loop inside the agent session.

Two properties make this better than generic RAG-over-docs progressive disclosure: the layers have
**verified correctness** (a fetched claim is backed by a green blind test, not by an LLM's
summary), and the vocabulary is **closed** (every reference resolves, so an agent can navigate
mechanically without semantic guessing). The knowledge graph above would serve as the MCP's index
layer — queries 2, 4, and 5 are graph traversals.

## Comparison with Understand-Anything

The two tools look adjacent ("LLM pipeline turns a codebase into structured knowledge") but sit at
nearly opposite poles of the design space.

| Dimension | Airlift | Understand-Anything |
|---|---|---|
| Object of extraction | Business *behavior* (claims, rules, invariants) | Code *structure* (files, functions, imports, layers) + summaries |
| Ground truth | Executable: blind tests green/red against live code | Deterministic parse (tree-sitter) for edges; unverified LLM prose for semantics |
| Correctness check | E1–E4 falsifiable proofs; mutation testing | `graph-reviewer` checks referential integrity/completeness of the *graph*, not truth of the *summaries* |
| Coverage | One seam, deep (34 claims, 772 lines) | Whole codebase, shallow (every file gets a node) |
| Cost profile | High per-seam, hand-built harness | One `/understand` run + incremental updates, no harness |
| Consumer | Agents (blind testgen, refactoring, migration) | Humans (dashboard, tours, onboarding, personas) |
| Drift handling | Spine: red tests name violated claims | Fingerprint-based re-analysis of changed files |
| Architecture stance | Deliberately excluded from claims (must survive a rewrite) | The main subject (layers, dependency graph) |

**Where Understand-Anything is genuinely strong and Airlift is weak:** breadth per dollar. Its
tree-sitter/LLM split is smart engineering — structural edges are deterministic and reproducible,
LLM effort is spent only on semantics, and incremental fingerprinting keeps it cheap to maintain.
It covers 200k lines in one run; Airlift covered 772 lines with a hand-built harness. Its
committed-graph + no-LLM viewer distribution story is also something Airlift lacks entirely. And
notably, its `domain-analyzer` (domains → flows → steps) is reaching toward Airlift's territory
from the cheap side.

**Where the approaches fundamentally diverge:** Understand-Anything's semantic layer is
*unverified narration*. A file summary that's subtly wrong, a domain flow that misses a branch, a
layer assignment that's misleading — nothing in the pipeline can detect it, because nothing
executes. Its graph-reviewer validates that the graph is well-formed, not that it's true. Airlift's
entire contribution is refusing that trade: a claim isn't in the IR until a blind agent reproduced
the behavior from it. The cost is 100× per line; the payoff is that the artifact can be
*load-bearing* — you can refactor, migrate, or gate PRs against Airlift claims, whereas you can
only *read* an Understand-Anything graph. Put differently: UA answers "what is this code and how
does it fit together?"; Airlift answers "what must remain true when this code changes?" — and only
Airlift can prove its answer.

**They compose rather than compete.** The practical synthesis for Airlift's roadmap:

1. **UA as the seam-finder.** Airlift's biggest unscaled cost is choosing and briefing seams. UA's
   layer graph + domain flows are exactly the map you'd use to rank seams by business-logic density
   and blast radius — run UA broad, run Airlift deep on the seams that matter (the handoff's
   thread 3 target selection could literally be graph queries).
2. **UA's graph substrate as Airlift's knowledge-graph skeleton.** UA already has node/edge types,
   persistence, embedding search, and a dashboard. Airlift claims could be a node type (its wiki
   mode already extracts "claims" and entities, structurally); the difference is Airlift's would
   carry `verified` status and test bindings. The dashboard's tour machinery would render claim
   clusters — "the exemption logic, ordered by `depends_on`" — for free.
3. **Airlift as UA's missing verification tier.** In a combined system, UA's cheap summaries are
   tier-0 understanding, and Airlift claims are tier-1 *warranted* understanding, with the
   blind-test spine as the promotion mechanism. That tiering is also exactly the
   progressive-disclosure MCP story: fetch cheap structure first, escalate to verified behavior
   when the stakes (a migration, a PR gate) demand it.

**The one-line verdict:** Understand-Anything optimizes for *breadth of comprehension* and trusts
the LLM's narration; Airlift optimizes for *depth of guarantee* and trusts nothing it hasn't
executed. Airlift's open problem is scaling breadth (harness cost, seam selection, glossary
governance); UA's is that nothing it says is checked. Each one's weakness is the other's core
competence.
