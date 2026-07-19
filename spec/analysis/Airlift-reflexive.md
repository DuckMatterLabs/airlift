---
confidence: analyzed
related: [spec/planning/Airlift-m2-freeze.md, spec/planning/Airlift-m3-second-target.md, spec/drafts/Airlift-IR.md]
sources: [spec/drafts/Airlift-IR.md, spec/drafts/Airlift-article.md, ir-spec/IR-SCHEMA.md, tests/out/ofbiz-tax/RUN-REPORT.md, spec/analysis/IR-analysis.md, spec/analysis/socraticode-analysis.md]
trajectory: >
  Normative for M2/M3; AADR register materializes as spec/adr/ at M2; risks
  R1-R5 await falsifier evidence from M3; re-derivation probes due ~M3 and ~M7.
---

# Airlift on Airlift — the WHYs of our own design, and the local-optimum problem

Date: 2026-07-19. Companion to `spec/planning/MILESTONES.md`; normative input to M2 (the freeze) and
M3 (the second target). Sources: `spec/drafts/Airlift-IR.md`, `spec/drafts/Airlift-article.md`,
`ir-spec/IR-SCHEMA.md`, `tests/out/ofbiz-tax/RUN-REPORT.md`, `spec/analysis/IR-analysis.md`,
`spec/analysis/socraticode-analysis.md`.

## 1. The reflexive principle

Airlift's founding critique of target codebases is: *code is a position, not a
trajectory; the WHYs evaporate; agents grounded on the snapshot photocopy the photocopy.*

That critique applies to Airlift itself, today, with embarrassing precision. Goal 1 was a
spike: design decisions were made fast, under empirical pressure, and their rationale
lives in narrative artifacts (`RUN-REPORT.md`, `spec/planning/Goal1-handoff.md`, chat history) — exactly the
"archaeology" regime we deplore in our targets. Six months from now, "why constrained
Gherkin and not state machines?" will be answerable only by the kind of history-mining
Goal 2 builds for other people's code.

So we dogfood: **every load-bearing design decision in Airlift gets a self-ADR** — the
decision, the alternatives actually considered, the evidence that decided it, and
(the part ordinary ADRs omit) a **falsifier**: the observation that would reopen it.
The falsifier is what turns an ADR from a justification into a claim. An ADR without a
falsifier is marketing; an ADR with one is science — the same move that distinguishes
Airlift's IR from a wiki.

Discipline for the register (§5): each entry is tagged **load-bearing** (chosen against
alternatives, for reasons that still apply) or **incidental** (an accident of the spike
that survived because nothing pushed back). This mirrors the boilerplate discipline in
`spec/drafts/Airlift-IR.md` — *extraction never discards, it classifies* — pointed at ourselves.
Incidental decisions are precisely the ones a freeze must not accidentally canonize; they
are our own "bug-compatible behavior" (IR-analysis pitfall 3, reflexively applied: which
of our design choices are accidents we're preserving because tests now pass against
them?).

## 2. Why the current IR format — the record

Each decision, its rejected alternatives, and the evidence. (These become the seed
self-ADR register in §5.)

**D1 — Claims as constrained Gherkin + decision tables, not pseudo-code, not prose.**
`spec/drafts/Airlift-IR.md` disqualified both alternatives *a priori*: pseudo-code is unexecutable
yet code-shaped (re-imports incidental structure); prose puts a generative LLM step on
the truth path (same claim → different tests on different days). Constrained Gherkin is
the narrow overlap: deterministic enough to compile to tests, readable enough for human
ratification, writable enough for LLM extraction. *Evidence:* E1's repair loop worked at
the claim level — ambiguities were fixable as text edits a fresh agent then reproduced
correctly. *Status: load-bearing.*

**D2 — Closed glossary; every claim noun must resolve.**
Chosen for lossless semantics ("Lingua franca" pillar) and mechanical navigability (no
dead ends — the property the MCP design now leans on). Alternative — open vocabulary +
embedding similarity — was implicitly rejected: similarity is exactly the "agent bridges
the gap by guessing" failure. *Evidence:* blind agents used glossary terms correctly
35/35; the `not:` fields carried real disambiguation weight (tax-exemption ≠ zero rate).
*Status: load-bearing. Known cost:* governance at scale (IR-analysis pitfall 4) — the
falsifier below.

**D3 — One atom per file, flat YAML, graph as a view.**
Progressive disclosure, git-diffable ratification units, no database on the truth path.
The graph/index is always a rebuildable projection (two-layer store, whitepaper §I).
*Evidence:* per-claim repair and per-claim spine reporting both exploited file
granularity. *Status: load-bearing as a principle (files = event log); incidental in its
current flatness* — directory sharding/namespacing will be forced by scale, and that's a
delta, not a break.

**D4 — Sufficiency proven operationally (E1–E4), not argued.**
Losslessness is not a format property but a round-trip demonstration (bisimulation).
This is the project's identity decision — the one that separates it from UA-style
narration. *Status: load-bearing, constitutive. But note:* the proof stack is
sufficiency-only; completeness is unmeasured (see §4, R1).

**D5 — The IR is a *stack of typed claim-kinds*; Goal 1 implemented ONE kind.**
This is the most important honesty in this document. `spec/drafts/Airlift-IR.md` designed a stack:
behavior rules (Gherkin) — **implemented**; domain schema (typed constraints) —
partially (domain-model.yaml, fixture vocabulary); invariants (property-based tests),
processes (state machines / model-based tests), contracts (OpenAPI/IDL) — **not
implemented**. The tax seam happened to be expressible almost entirely as behavior rules
+ decision tables, so the spike never hit the walls that would have forced the other
kinds. **The danger: freezing "the IR" as "the one kind the first seam needed."** The
schema that M2 tags v1.0 must freeze the *stack frame* (claim-kind envelope, statuses,
traceability, glossary binding) while explicitly marking the kind-set as open — adding a
claim-kind is a minor version, by design. *Status: the stack is load-bearing; the
current one-kind population is incidental.*

**D6 — Fused co-evolution of IR and pipeline (spec/planning/goals.md's Goal-1 argument).**
The IR earned its shape empirically by hitting real walls, instead of being designed in
isolation for imagined behaviors. *Evidence:* the E1 red→IR-repair loop is the method
working as designed. *Status: load-bearing as an epistemic strategy — and it has an
expiry:* co-evolution ends at M2. After the freeze, the same empiricism must arrive
through versioned deltas triggered by new targets (M3) — which is exactly the
local-optimum trap §4 examines.

**D7 — Copilot+Opus executes, Claude authors; blind means blind; red blind test ⇒ fix
the IR never the test; determinism on the truth path.**
The standing invariants. Each is load-bearing with a documented rationale in
spec/planning/MILESTONES.md. One carries a known confound: E1's generator and the extractor share
model priors (IR-analysis pitfall 5) — falsifier scheduled (§4, R2).

## 3. How the IR is meant to evolve

Three sanctioned mechanisms, in increasing severity:

1. **Minor deltas** (additive): new claim-kind, new optional field, new status value.
   Versioned changelog entries; validators extended, never weakened. Triggered by target
   friction (M3's friction log is the intake queue).
2. **Major versions** (breaking): field semantics change, kind restructuring. Requires a
   migration tool for existing IR *and* a rerun of the affected proofs (the migrated
   ofbiz-tax IR must still pass E1-blind with a fresh agent — migration is itself a
   change on the truth path and gets the same treatment as any drift).
3. **Basis re-derivation** (the escape hatch — see §4 countermeasure C3): a scheduled,
   deliberate challenge to the schema's foundations, not triggered by any incremental
   signal.

What is *not* sanctioned: silent renegotiation (the M2 freeze exists to end it), and
schema growth driven by speculative generality ("Fineract might need X") — walls must be
hit, not imagined (D6's lesson).

## 4. The local-optimum problem, taken seriously

The user's question, sharpened: Airlift now optimizes itself against E1–E4 plus
incremental deltas. Hill-climbing has two classic failure modes, and we are exposed to
both.

### Failure mode A — the fitness function is wrong or partial

E1–E4 measure *sufficiency of the IR for reproducing named behaviors on one seam under
one model family*. Known gaps, stated as risks with falsifiers:

* **R1 — Sufficiency ≠ completeness.** A behavior never fragmented is invisible to every
  downstream check; E2's mutations were planted where claims already existed.
  *Falsifier:* full-seam systematic mutation testing (M3 stretch exit) — if the
  caught-fraction is low, our headline proofs were measuring the easy half.
* **R2 — Double-LLM prior leakage.** Extractor and blind generator share Opus priors; an
  ambiguous claim can pass E1 because both ends fill the gap identically. *Falsifier:*
  cross-model E1 (different model family generates the blind suite). Cheap; scheduled
  into M3. If cross-model E1 goes materially redder than same-model E1, "blind" was
  partially theater and the IR needs tightening the proofs couldn't see.
* **R3 — One-pathology fitness.** OFBiz is entanglement; every prompt, validator, and
  the seam-brief genre were tuned on it. Fineract's over-abstraction may reward the
  *opposite* extraction moves. *Falsifier:* M3's friction log — if generality requires
  rewriting prompts rather than parameterizing them, the "target-agnostic pipeline" claim
  was overfit.
* **R4 — Expressiveness ceiling as invisible selection bias.** We may be unconsciously
  choosing seams that constrained Gherkin can express (the streetlight effect). Temporal
  properties, concurrency, cross-service invariants, probabilistic behavior have no
  claim-kind today (D5). *Falsifier:* deliberately select an M3+ seam that *should*
  require a state-machine or property claim-kind; if we route around it instead, the
  bias is confirmed.
* **R5 — Harness-cost assumption.** The per-seam hand-built harness is treated as a
  constant of nature; if it stays unautomated, Airlift's economic claim fails at
  codebase scale regardless of how green the proofs are. *Falsifier:* M3's cost
  accounting; if harness cost doesn't drop materially with experience/scaffolding,
  the amortization story needs a redesign, not a delta.

### Failure mode B — the search strategy is greedy

Post-freeze, all sanctioned evolution is deltas — local moves. Some better designs are
across a valley no delta path crosses: e.g., claims as typed objects in a proper logic
(executable specifications à la TLA+/Alloy) instead of Gherkin; or a glossary as an
ontology with formal subsumption instead of flat terms; or abandoning per-seam harnesses
for generated property-based harnesses. Incremental optimization will never propose
these, *by construction* — each intermediate step is worse than both endpoints.

### Countermeasures (the actual answer)

* **C1 — Record WHYs with falsifiers now** (§5 register, materialized in M2). Lock-in is
  most dangerous when a frozen decision's rationale is forgotten and it becomes
  unquestionable *scripture*. A decision with a recorded falsifier stays a *claim* —
  challengeable by evidence forever. This is the single cheapest insurance.
* **C2 — Keep out-of-band fitness signals** that the optimization loop does not control:
  cross-model E1 (R2), full-seam mutation score (R1), second-target friction log (R3),
  harness cost accounting (R5). A hill-climber can't game a measure it isn't climbing.
* **C3 — Scheduled basis re-derivation: the blind re-design probe.** Airlift's own
  method, applied at the design level. Periodically (once per major milestone era, ~M3
  and ~M7), a fresh agent receives the *problem statement* (`spec/drafts/Airlift-IR.md` §Problem +
  the exit-criteria philosophy) and the accumulated falsifier evidence — but **not the
  current schema** — and derives an IR design from scratch. Diff against the incumbent.
  Three outcomes, all valuable: convergence (the design is re-derived → strong evidence
  we're at a good optimum, not just a defended one); divergence on incidentals (cheap
  deltas); divergence on load-bearing structure (the valley-crossing signal no
  incremental process can generate — triggers a major-version debate with evidence on
  the table). This is E1 for the schema itself: if the design is only reproducible *with*
  the design in hand, we've been grading our own homework.
* **C4 — Major versions are legitimate, budgeted, and pre-equipped.** The migration-tool
  + re-prove requirement (§3.2) makes valley-crossing *expensive but chartered*. The
  worst lock-in is cultural: a freeze that makes breaking changes unthinkable. M2's
  freeze text must say explicitly: *v1.0 is a commitment device against silent drift,
  not a claim of final correctness.*
* **C5 — The tier system already contains the answer to "codified wrong ideas."**
  Airlift never claims its IR is *true* — it claims it is *attested at a tier*. The same
  epistemics applies reflexively: v1.0 is `pinned` (survived one seam's full proof
  stack), not `verified` (survived multiple pathologies, cross-model, mutation-swept).
  Schema versions should carry exactly this status metadata about themselves.

## 5. The self-ADR register (seed — M2 materializes these as `spec/adr/AADR-NNN.md`)

| ID | Decision | Tag | Falsifier (reopens it) |
|---|---|---|---|
| AADR-001 | Claims = constrained Gherkin + decision tables (D1) | load-bearing | A seam whose behavior can't be expressed without prose escape hatches (R4) |
| AADR-002 | Closed glossary, mandatory resolution (D2) | load-bearing | Glossary governance cost grows super-linearly at 2nd/3rd target; cross-domain polysemy unmanageable in flat namespace |
| AADR-003 | One atom per file; flat YAML; graph is a view (D3) | principle load-bearing; flatness incidental | Navigation/coverage queries become impractical without materialized graph as truth (must never happen — if it does, the two-layer store thesis failed) |
| AADR-004 | Operational sufficiency proofs E1–E4 as the quality gate (D4) | constitutive | Cross-model E1 or mutation sweep shows proofs passing while IR is materially ambiguous/incomplete (R1, R2) |
| AADR-005 | IR = open stack of typed claim-kinds; v1 populates one kind (D5) | stack load-bearing; population incidental | A needed kind can't be added as a minor delta (envelope was over-fit to Gherkin) |
| AADR-006 | Fused co-evolution until freeze; deltas after (D6) | load-bearing, expired at M2 | M3 friction log shows delta process too slow/coarse for real target friction |
| AADR-007 | Copilot+Opus executes; Claude authors (D7) | incidental (client mandate of the spike) | Harness/model landscape shift; cross-model results (R2) may *strengthen* to load-bearing or retire it |
| AADR-008 | Blind repair feedback = compile errors only | load-bearing | Evidence that compile-only feedback systematically under-repairs while remaining non-leaking alternatives exist |
| AADR-009 | Per-seam hand-built harness (fixture DSL + contract doc) | incidental (spike economics) | R5 cost accounting; harness scaffolding generation (IR-analysis optimization 3) succeeding retires this |
| AADR-010 | Bug-compatible extraction without a queryable fidelity marker | incidental — known debt | First real consumer needs to distinguish rule from accident: add `fidelity:` field (candidate first minor delta) |
| AADR-011 | JUnit-3-idiom spine binding via `@AirliftClaim` annotations | incidental (OFBiz's harness reality) | Second target's test stack forces the binding mechanism into the target plugin where it belongs |

Register rules: new load-bearing decisions require an AADR at merge time (enforced as a
checklist item in M2's delta process); falsifier observations are recorded on the AADR
when they occur, whether or not they trip it; a tripped falsifier obligates a review, not
automatically a reversal.

## 6. Summary answer to the question posed

*Why this IR?* Because both obvious alternatives (pseudo-code, prose) fail the
determinism-on-the-truth-path requirement, and the chosen form earned its shape
empirically against a real seam (D1–D6) — with the honest caveat that one seam populated
one claim-kind of a designed multi-kind stack (D5).

*How does it evolve?* Minor deltas from target friction; major versions with migration +
re-proof; and scheduled basis re-derivation as the deliberate valley-crossing mechanism
(§3, C3).

*What if we've codified wrong ideas?* Then incremental optimization alone would entrench
them — which is why the defense is not better increments but (a) WHYs recorded as
falsifiable claims, (b) fitness signals outside the optimization loop, (c) a chartered,
budgeted path for breaking changes, and (d) the blind re-design probe that asks, at every
era boundary, whether a fresh mind re-derives our schema from the problem — the same
question E1 asks of every claim. Airlift's central insight, applied to itself: *don't
trust what you haven't tried to falsify.*
