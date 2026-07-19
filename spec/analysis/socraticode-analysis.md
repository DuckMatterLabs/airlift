---
confidence: analyzed
related: [spec/analysis/IR-analysis.md, spec/planning/Airlift-goal-4-mcp.md, SocratiCode]
sources: [spec/planning/Goal1-handoff.md, spec/analysis/IR-analysis.md, /Volumes/Dancer/Develop/AIRLIFT/socraticode]
trajectory: >
  Borrow/refuse decisions consumed by Goal 4 spec; three-way map feeds the
  reflexive doc. Revisit if SocratiCode adds generative layers.
---

# Airlift vs SocratiCode — critical analysis and comparison

Date: 2026-07-19. Sources: `spec/planning/Goal1-handoff.md`, `spec/analysis/IR-analysis.md`, and the SocratiCode repo
(clone at `/Volumes/Dancer/Develop/AIRLIFT/socraticode`,
upstream: <https://github.com/giancarloerra/socraticode>). Companion to `spec/analysis/IR-analysis.md`
(the Understand-Anything comparison); this doc assumes that one has been read.

## What SocratiCode actually is

SocratiCode is an MCP **codebase context engine**: hybrid semantic + BM25 (RRF-fused) search
over AST-aware chunks stored in Qdrant, a polyglot import/dependency graph, a symbol-level
call graph (`codebase_impact` blast radius, `codebase_flow` entry-point tracing,
`codebase_symbol` callers/callees), and "context artifacts" (DB schemas, API specs, infra
configs indexed alongside code). Zero-config Docker infrastructure, incremental/resumable
indexing, file watcher, multi-agent shared index, cross-project and branch-aware modes.
Shipped as an npm MCP server, Claude Code plugin (skills + explorer agent), and VS Code
extension. Claims production use on ~40M-line repos.

**The decisive architectural fact (verified in `src/`): there is no generative LLM anywhere
in the pipeline.** The only model calls are embedding providers
(`provider-ollama.ts`, `provider-openai.ts`, `provider-google.ts`, …). Chunking is ast-grep;
the import graph and symbol graph are static analysis (`graph-imports.ts`,
`graph-symbol-resolution.ts`); search is vector + BM25 fusion. Every artifact SocratiCode
produces is either deterministic structure or a similarity ranking. **All semantics are
deferred to the querying agent at read time.**

This places it at a *third* pole of the design space, distinct from both Airlift and
Understand-Anything:

- **Airlift**: precompute *verified semantics* (claims proven by blind tests). Expensive,
  narrow, load-bearing.
- **Understand-Anything**: precompute *unverified semantics* (LLM narration over a
  deterministic skeleton). Cheap, broad, trust-me.
- **SocratiCode**: precompute *no semantics at all* — only deterministic structure and a
  retrieval index; let the consuming agent derive semantics fresh each session. Cheapest,
  broadest, and epistemically the cleanest of the three cheap options: it never asserts
  anything that could be false.

Despite the tagline ("Your AI reads code. SocratiCode understands it."), SocratiCode does not
understand code — it makes reading code *efficient*. That is not a dig; it's the design. But
it means the marketing inverts the reality: SocratiCode is precisely the tool for AIs that
read code, and the "understanding" lives and dies with each agent session.

## Comparison

| Dimension | Airlift | SocratiCode |
|---|---|---|
| Object produced | Behavioral contract (claims, glossary, traceability) | Retrieval index (chunks, embeddings, import/symbol graphs) |
| Semantic content | Verified, executable (blind tests green/red) | None — semantics re-derived by the agent per query/session |
| Can it be wrong? | Yes, and the blind loop *detects* it (E1 30/35 → repair) | Structure: rarely (static analysis). Retrieval: yes, silently — a recall miss has no red test |
| Ground truth | Execution against live code | Syntax (AST, imports) + embedding similarity |
| Unit of knowledge | Claim atom (`TAX.EXEMPT.ZEROING`) | Chunk / file / symbol |
| Impact analysis | Semantic: red test → violated *claim* names | Syntactic: blast radius = files/functions that transitively reference a symbol |
| Survives a rewrite? | Yes — that's the point (E3: 675-line refactor, claims untouched) | No — the index *is* the code; a rewrite re-indexes into a different artifact |
| Drift handling | Spine: verification status per claim | File watcher: index freshness per file (freshness ≠ correctness — there's nothing to be correct about) |
| Cost profile | High per-seam, hand-built harness, LLM-heavy | Near-zero marginal: one embedding pass, incremental after; no generative tokens |
| Coverage | One seam deep (34 claims / 772 lines) | Whole codebase, uniformly shallow (every chunk indexed, 40M LoC claimed) |
| Accumulation | Knowledge compounds: claims persist and gate future changes | Nothing accumulates: each session re-spends tokens deriving the same understanding, cheaper |
| Consumer | Agents doing load-bearing work (migration, PR gates, blind testgen) | Agents doing exploration (navigation, orientation, blast-radius triage) |
| Distribution | None (uncommitted repo, bespoke runners) | Mature: npm, plugins, skills, extension, multi-host, multi-agent locking |
| Language scope | Target-plugin per seam (Java/OFBiz today) | 18+ languages out of the box, polyglot |

## Critical assessment of SocratiCode (through Airlift's lens)

**1. "No semantics" is a strength Understand-Anything lacks — and a ceiling Airlift exists to
break.** SocratiCode never lies: unlike UA's summaries, there is no narration to be subtly
wrong. But it also never *knows*. The agent's understanding is rebuilt every session from
retrieved snippets, bounded by the same model priors and reading errors every time, and
evaporates when the session ends. Airlift's central bet — that behavioral knowledge should be
extracted once, verified, and *persisted as an asset* — is exactly the layer SocratiCode
deliberately refuses to build. Neither refutes the other; they answer different questions.
SocratiCode answers "where is the relevant code?"; Airlift answers "what must remain true when
it changes?"

**2. Blast radius is not impact.** `codebase_impact target=validateUser` returns every file
that transitively calls `validateUser` — syntactic reachability. It cannot distinguish a
behavior-preserving refactor (E3: 675 lines churned, zero behavioral impact — SocratiCode
would report a large blast radius and re-index everything) from a one-character semantic flip
(E4: tiny diff, four `TAX.EXEMPT` claims violated — SocratiCode would report a trivial blast
radius). Airlift's spine gets the sign right in both cases *because it executes*. The
symmetric weakness: a mutation in un-claimed logic is invisible to Airlift (IR-analysis
pitfall 2) but still inside SocratiCode's blast radius. Syntactic impact over-approximates;
claim-based impact under-approximates to the claim set. A serious change-gating system wants
the intersection: syntactic radius to *scope* the check, claims to *decide* it.

**3. Retrieval failure is silent, and nothing in the system can notice.** Airlift's defining
property is that its artifact is falsifiable (blind tests). SocratiCode's search quality has
no analogous gate: if the embedding model ranks the relevant chunk 47th, the agent simply
never sees it and proceeds confidently on partial context. The README's benchmark (61% less
context, 84% fewer tool calls, 37× faster on VS Code's repo) is self-reported, single-repo,
single-model, and measures *efficiency*, not *answer correctness* — precisely the metric an
Airlift-style evaluation would refuse to accept as primary. A fair note in the other
direction: SocratiCode's deterministic layers (import graph, symbol graph) *are*
reproducible and testable, and its repo carries a real test suite for them — the unverifiable
part is only the retrieval relevance, whereas UA's unverifiable part was the content itself.

**4. Where SocratiCode is simply better, and Airlift shouldn't compete:** breadth per dollar
(no generative tokens at all — strictly cheaper than UA, let alone Airlift), maintenance
(watcher + incremental + resumable checkpoints vs Airlift's "rerun the pipeline"),
concurrency (multi-agent shared index with cross-process locking — Airlift has no multi-agent
story), and productization (Airlift's `spec/analysis/IR-analysis.md` *designs* a progressive-disclosure
MCP; SocratiCode has *shipped* one, with plugins, skills, a delegatable explorer agent, and
host-agnostic distribution). The engineering maturity gap is large and worth stealing from,
not resenting: SocratiCode's tool-surface design (status/index/search/graph/impact/flow with
`projectPath` defaulting to cwd, background indexing with progress polling) is a working
template for the `ir_overview`/`list_claims`/`get_claim`/`trace`/`verify` MCP that
IR-analysis sketches.

**5. The "smaller models" claim cuts both ways.** SocratiCode argues that precomputing hard
structure (blast radius, call-flow) lets smaller models do architecturally complex tasks.
Airlift's E1 is the strong version of the same thesis: precomputed *verified behavior* let a
sandboxed agent with no code access write 35 correct tests. The difference is what gets
precomputed — structure (cheap, syntactic, per-file) vs behavior (expensive, semantic,
per-seam). SocratiCode's version scales today; Airlift's version is the only one that
survives the code being rewritten.

## Critical reflection on Airlift (through SocratiCode's lens)

- **Airlift's exploration stages are artisanal where they could be indexed.** Stage 1
  (fragment map) and seam briefing currently rely on an LLM agent reading source under
  Copilot. A SocratiCode-style index over OFBiz would make fragment discovery, cross-reference
  checks ("who else calls `getTaxAdjustments`?"), and especially **seam selection for the
  second target** (handoff thread 3) into cheap deterministic queries — `codebase_impact` over
  candidate service methods is a ready-made "blast radius × business-logic density" seam
  ranker.
- **Airlift has no freshness story between pipeline runs.** SocratiCode's watcher keeps its
  index live at every file change; Airlift's IR is only re-validated when someone runs the
  spine. The claim-status promotion tool (handoff thread 4) plus a watcher-style trigger
  ("`TaxAuthorityServices.java` changed → re-run bound spine tests → demote/confirm claim
  statuses") is the composition of the two drift models: SocratiCode-style *detection* of
  change, Airlift-style *adjudication* of whether it mattered.
- **Airlift has no distribution or multi-consumer story.** One repo, bash runners, uncommitted.
  SocratiCode demonstrates what "the context lives with the codebase, not with the assistant"
  looks like operationally (committed `projectId`, team-shared index, host-agnostic MCP).
  Airlift's IR — small, textual, verified — is *more* portable than an embedding index, and
  currently less distributed.

## Composition (concrete, near-term)

1. **Airlift IR as SocratiCode context artifacts — the literal integration.** SocratiCode
   already indexes non-code knowledge (schemas, API specs) via
   `.socraticodecontextartifacts.json`. `tests/out/ofbiz-tax/ir/**/*.yaml` is exactly such an
   artifact set: registering the claims, glossary, and traceability files would make
   `codebase_context_search "tax exemption"` return *verified claims ranked above raw code* —
   tier-1 warranted knowledge surfaced through tier-0 retrieval infrastructure, today, with
   zero new code on either side.
2. **SocratiCode as Airlift's stage-1/seam-selection substrate** (per above): index the target
   repo once, drive fragment mapping and second-target ranking from `codebase_impact` /
   `codebase_flow` / `codebase_symbol` instead of agent file-crawling.
3. **Symbol graph → claim graph bridge.** Airlift traceability binds claims to fragments
   (files/symbols); SocratiCode's symbol graph knows the callers of those symbols. Joining
   them answers the killer query from IR-analysis §MCP layer 5 — "I'm editing
   `handlePartyTaxExempt`, what am I about to violate?" — as: SocratiCode resolves the edit
   site to symbols → traceability maps symbols to claims → spine runs the bound tests. Each
   system contributes the half the other lacks.
4. **What not to copy:** SocratiCode's decision to defer all semantics is right *for its
   product* and wrong for Airlift's. The temptation after reading its codebase is to add an
   embedding index over claims ("semantic claim search"). Resist making that the primary
   interface: the closed glossary + self-describing `DOMAIN.AREA.SLUG` IDs already give
   deterministic, dead-end-free navigation (IR-analysis §MCP), which is strictly stronger
   than similarity ranking for a vocabulary this small. Embedding search over claims is a
   nice-to-have entry point, not the spine.

## The three-way map

With UA and SocratiCode both on the board, the design space has a clean shape — what is
precomputed, and what warrant it carries:

| | Precomputes | Warrant | Fails by |
|---|---|---|---|
| SocratiCode | structure + retrieval index | deterministic (structure); none (relevance) | silent recall misses; semantics re-derived per session |
| Understand-Anything | structure + narrated semantics | deterministic (structure); none (narration) | confident wrong prose nothing can detect |
| Airlift | verified behavioral semantics | executable proof (E1–E4) | cost; coverage limited to claimed behaviors |

UA occupies the awkward middle: it pays LLM cost to produce assertions it cannot check.
SocratiCode and Airlift are the two *coherent* poles — assert nothing, or assert only what
you've proven — and they compose along a clean seam: SocratiCode finds and scopes; Airlift
warrants and gates. The stack that beats all three alone: SocratiCode's index for
navigation and change detection, Airlift's claims as the verified tier registered as context
artifacts, and the spine as the promotion/adjudication mechanism between them.

**One-line verdict:** SocratiCode is retrieval infrastructure that makes agents efficient at
re-deriving understanding they will never keep; Airlift is a distillery that makes
understanding permanent and provable but only where you can afford it. SocratiCode's honesty
is refusing to assert; Airlift's honesty is proving what it asserts — and the integration
point (verified claims served through commodity retrieval infrastructure) is unusually
concrete, down to an existing config file format.
