---
confidence: proven
related: [spec/index.md, CLAUDE.md, spec/planning/Goal1-handoff.md]
sources: [session transcripts, spec/planning/Goal1-handoff.md]
trajectory: >
  Append-only forever; entries never rewritten; superseded operational detail
  moves to Goal<N>-handoff docs.
---

# spec/log.md — cross-session memory

Append-only, newest entry first. One entry per working session that changed anything.
Format: date, who/what harness, then: **Done** (facts, with pointers — no narratives),
**Decisions** (anything a future session must not silently re-litigate),
**Open** (threads left hanging). Keep entries terse; evidence lives where it lives —
this log holds pointers, not content. A session that only reads appends nothing.

---

## 2026-07-19 (g) — Claude session: human-facing root README

**Done**

* Old Goal-1-spike README moved to `tests/runs/goal1-spike-README.md` (frontmatter
  backfilled; frozen as run-adjacent reference — component map + proof-run summary).
* New root `README.md` written for humans: the position-not-trajectory problem,
  the falsifiable-claims bet with the E1–E4 results table, the CDT genus/species
  framing (provable-vs-traceable, inversion-vs-mirror, fact/intent split), the
  clerk-not-oracle indemnity story, pipeline overview, status/roadmap, reading order.
  Sources: manifesto (`drafts/Airlift-article.md`), `analysis/IR-analysis.md` §CDT,
  M1/M2 evidence.

**Decisions**

* Root README is the human-facing pitch, updated at milestone boundaries; operational
  truth stays in `Goal1-handoff.md`; the manifesto remains the protected founding
  prose. (Log (b)'s "README stays at repo root" decision still holds — a README does.)

**Open**

* None new.

## 2026-07-19 (f) — Claude session: Copilot takeover briefing

**Done**

* `Goal1-handoff.md` expanded with §8 (M2 addendum + takeover protocol for Copilot
  sessions assuming both roles per AADR-007): session protocol, frozen-schema/promotion
  discipline, stage-4/drill mechanics, M2 gotchas, M3 notes. Stale §2/§6 entries
  patched (threads 4–6 marked done); frontmatter backfilled.
* Root `AGENTS.md` created: Copilot CLI does not read `CLAUDE.md`, so interactive
  Copilot dev sessions need this pointer to the constitution + orientation order.
  Pipeline stage agents are unaffected (`--no-custom-instructions`).

**Decisions**

* **Copilot+Opus 4.8 is the development harness going forward** (user directive), both
  author and executor roles — the AADR-007 corp-deployment scenario is now the actual
  configuration. Consequence noted in handoff §8.5: R2 (double-LLM prior leakage) is
  live; M3's cross-model E1 gains importance.
* M3 target: **real production code** (user directive) — supersedes the M3 spec's
  Fineract recommendation; record the concrete target in the spec when chosen.

**Open**

* M3 unstarted; next session starts at `spec/planning/Airlift-m3-second-target.md` +
  handoff §8.5.

## 2026-07-19 (e) — Claude session: M2 EXECUTED — freeze & harden, all exits met

**Done**

* **M2 complete** (spec: `planning/Airlift-m2-freeze.md`; full evidence:
  `tests/runs/m2-freeze-report.md`). Schema frozen at **v1.0**: `ir-spec/VERSION`,
  `ir-spec/CHANGELOG.md`, `ir-spec/IR-SCHEMA.md` rewritten (§0 version declaration,
  §11 change process, §12 statuses/attestations/promotion, §13 frozen-vs-open,
  §14 AADR traceability). All five validators version-check `schema_version`; the five
  ofbiz-tax stage artifacts stamped `"1.0"`; all validate clean.
* `pipeline/promote.py` (+ junitxml.py, stamp-backfill.py, 29 unit tests): 27 claims
  → `verified`, 3 `pinned`, 4 `extracted`, zero manual edits, second run byte-level
  no-op; sha-stamped attestations on every promoted claim. Evidence chain archived in
  `tests/runs/m2-promotion/` (fresh green suite reruns: backfill 19/19, blind 35/35).
  Target-neutral spine files via `report-claims.py --spine-out` (AADR-011 upheld).
* Stage-4 external runner in `run-pipeline.sh` (runner compiles/runs/stamps `run:`;
  agent never runs gradle); demonstrated green with the real committed suite
  (`tests/runs/m2-ext-runner-demo.log`). Full agent-driven stage-4 rerun deliberately
  deferred to M3 (would overwrite the proven M1 backfill suite for a mechanism test).
* Distill repair loop demonstrated end-to-end through `run-pipeline.sh` (drill on a
  scratch target, `AIRLIFT_SEED_SCHEMA_VERSION=0.9` seam): a1 organic structural
  failure (traceability claims emitted as dict), a2 the seeded 0.9 rejection, a3
  repaired → green. `tests/runs/m2-repair-drill.log`, output in
  `tests/runs/m2-repair-drill-out/`. OFBiz tree stayed clean.
* Validator hardening beyond the spec (18 confirmed findings from a pre-execution
  adversarial review, all fixed): at-commit line coverage via `git show` with NO
  working-tree fallback (this also fixed the latent map-validator failure caused by
  stage-5's 30 annotation lines); `<skipped>` ≠ green; `pinned`/`verified` statuses
  require supporting attestations; manifest↔claim-file status consistency;
  observability enum enforced (schema erratum: `internal`, not
  `unobservable-via-contract`); YAML injection closed in spine/stamp writers;
  target-specific `OFBIZ_ROOT` removed from `pipeline/lib.sh`.
* Register: **AADR-012 added** (stable claim IDs; fragment lines = evidence-at-commit
  — surfaced by the §14 tracing pass); AADR-006 expired (freeze executed); AADR-010
  records the decision; AADR-011 observation appended.

**Decisions**

* **`fidelity:` deferred from v1.0** (M2 spec required decide-not-defer; the decision
  IS deferral): no consumer has tripped AADR-010's falsifier; reflexive §3 forbids
  speculative fields; the quirk is sub-claim-level so the marker would have nothing
  truthful to mark; an optional field is a cheap minor delta later. v1.1 candidate,
  recorded in `ir-spec/CHANGELOG.md` + AADR-010.
* Spec correction recorded (not silently edited): exit criterion 2's "35 verified"
  conflated blind test methods (35) with claims (34 total, 27 blind-bound). Also:
  criterion 1's "full pipeline rerun" read as validator rerun over the proven
  artifacts — regenerating the IR would destroy the artifacts criterion 2 depends on.
* Extraction stages now always emit `status: extracted` (stage-5 prompt); statuses
  move only via `promote.py` (schema §12). Promotion is forward-only; demotion is
  M7's, reading the same attestation records.

**Open**

* 4 claims remain `extracted` (TAX.CONTRACT.CALC-TAX, TAX.CONTRACT.CALC-TAX-FOR-DISPLAY,
  TAX.ORDER.VALUE-WEIGHT, TAX.VAT.PRICE-GROUP-SCOPE) and 3 `pinned` (TAX.DISPLAY.\*)
  — honest gaps: no blind binding. A blind-suite extension pass could close them (M3-adjacent).
* Repo uncommitted for this session's M2 work — user go-ahead pending (asked at
  session end). Proposed: conventional commits per workstream.
* v_ir's attempt-1 drill failure surfaced as a raw TypeError message (structural
  failure path) — functional but opaque; a friendlier structural-shape message would
  save a repair round's diagnosis time. Minor validator UX debt.

## 2026-07-19 (d) — Claude session: AADR register materialized as spec/adr/

**Done**

* Materialized the self-ADR register (seed: `analysis/Airlift-reflexive.md` §5) as
  `spec/adr/`: `README.md` (register table, rules, status legend, out-of-band signal →
  falsifier map) + `AADR-001…011-*.md`, one decision per file, each with decision, tag,
  alternatives actually considered, evidence (pulled from `Goal1-handoff.md`,
  `RUN-REPORT.md` pointers, `IR-analysis.md`), falsifier, and a dated
  falsifier-observations log. Done ahead of the M2 schedule (user request); M2's
  freeze work now updates the register rather than creating it.
* Enrichments beyond the seed table: AADR-003/010 record the CDT borrowings (typed
  edge vocabulary; `constrained-by` constraint node as the queryable fidelity home);
  AADR-004 records CDT Challenge XI as external validation and carries R1/R2 as known
  measurement gaps; AADR-011 separates the spine *principle* (load-bearing, lives with
  AADR-004) from the JUnit-3 binding *mechanism* (incidental).
* Cross-references updated: `spec/index.md` (new adr/ section), reflexive doc
  (frontmatter + §5 header: spec/adr/ now authoritative, §5 table is historical seed),
  `CLAUDE.md` (register location no longer conditional on M2).

**Decisions**

* Register materialization is early, not a scope change: register rules are verbatim
  from reflexive §5 (AADR at merge for load-bearing decisions; observations recorded
  whether or not tripped; tripped falsifier ⇒ review, not reversal).

* AADR-007 softened (user directive, same session): the load-bearing decision is the
  ROLE split — agent working on Airlift itself (authoring) vs. agent harness Airlift
  runs under (execution, defined by guarantees: fresh contexts, sandboxed blindness,
  batch prompt-in/artifact-out) — with the Claude/Copilot product binding demoted to
  incidental deployment detail. Rationale: corp deployment has no Claude, so Copilot
  assumes both roles; conversely Airlift may itself become the harness (Goals 4/7).
  Blindness/freshness are enforced by process, never by vendor split. R2 note: corp
  same-model-everywhere makes cross-model probes where-available insurance.

**Decisions (addendum)**

* Corp target environment: **Claude unavailable; Copilot fills both author and
  executor roles.** Constitution invariant 2's phrasing ("Copilot+Opus executes;
  Claude authors") now reads as the current engagement's binding, not the design —
  AADR-007 is the nuanced statement; constitution text left untouched pending user's
  call on rewording an invariant.

**Open**

* AADR files carry `status: active` prose only — no machine-readable status field
  beyond frontmatter confidence; fine until the promotion tool (handoff thread 4)
  wants to read them.

## 2026-07-19 (c) — Claude session: Code Digital Twin comparison

**Done**

* Read `spec/sources/DigitalTwin.pdf` (Peng & Wang, arXiv:2503.07967v4 — the
  whitepaper's sole reference) in full; compared against `drafts/Airlift-article.md`.
  Framing agreed with user: **CDT is the genus, Airlift a species** — same diagnosis
  and anatomy; Airlift adds three bets CDT lacks: provability (executable claims vs.
  traceable-only), source-of-truth inversion (vs. permanent mirror), and the
  fact/intent split (vs. one twin, one confidence economy). CDT's Challenge XI
  (realistic evaluation) independently calls for what the E1–E4 proving ground already
  does — citable validation of the methodology.
* `analysis/IR-analysis.md` gained a "Comparison with Code Digital Twin" section:
  component mapping table, divergences, and six borrowings — typed edge vocabulary
  (`justified-by`, `constrained-by` → Ledger/graph; constraint node as queryable home
  for `fidelity: bug-compatible`, AADR-010), structured context packages with token
  budgets (→ Goal 4 MCP/Maestro grounding), Twin-RAG graph-first retrieval, implicit
  feedback signals as curation input, the 11-challenge table as a MILESTONES audit
  rubric, and explicit per-claim version anchoring (needed for the indemnity tier-2
  promise). Explicit non-borrowings: single-twin unification, curation-only validation.

**Decisions**

* None normative; borrowings are recorded as analysis feeding M2 schema decisions and
  Goals 2/4 — not yet ratified into `ir-spec/`.

**Open**

* Non-functional constraints are absent from the IR schema entirely (surfaced by the
  CDT challenge checklist) — needs a deliberate include/delegate/reject decision at M2.

## 2026-07-19 (b) — Claude session: specs, reflexivity, reorganization

**Done**

* Critical analyses written: `analysis/socraticode-analysis.md` (three-way map:
  Airlift / Understand-Anything / SocratiCode); `analysis/IR-analysis.md` predates this
  session's second half.
* Detailed specs authored for everything after Goal 1: `planning/Airlift-m2-freeze.md`,
  `planning/Airlift-m3-second-target.md`, `planning/Airlift-goal-2-pr-history.md`,
  `planning/Airlift-goal-3-architecture.md`, `planning/Airlift-goal-4-mcp.md`,
  `planning/Airlift-goal-5-drift.md`.
* `analysis/Airlift-reflexive.md`: Airlift's own WHYs (D1–D7), local-optimum risks
  (R1–R5) + countermeasures (C1–C5), seed self-ADR register (AADR-001…011). Normative
  for M2/M3.
* spec/ reorganized: `planning/`, `analysis/`, `drafts/` (original prose).
  `MILESTONES.md` and `HANDOFF.md` moved into `spec/planning/`; HANDOFF then renamed
  `Goal1-handoff.md` — convention: each completed goal produces a `Goal<N>-handoff.md`.
  All cross-references rewritten; fixed a pre-existing broken link in
  `planning/Airlift-goal.md` (PR-history filename).
* Created root `CLAUDE.md` (constitution), `spec/index.md` (semantic map), this log.
* Constitution gained the frontmatter requirement (confidence / related / sources /
  trajectory on every generated MD); backfilled onto all 11 docs generated this
  session. Older docs (drafts, Goal1-handoff, MILESTONES, goal-1 spec) backfill
  opportunistically when next touched.
* Goals 6 (Glossary federation → M8) and 7 (Productionalization → M9) added per user
  directive: `planning/Airlift-goal-6-glossary.md`,
  `planning/Airlift-goal-7-productionalization.md`. Former "M8 — KB productization"
  renumbered/absorbed into M9; roadmap diagram updated.
* Whitepaper (`drafts/Airlift-article.md`) revised per user request to reflect the
  Airlift/Airbase split: new "The Airlift/Airbase split" section; goals paragraph,
  KB-comparison list, pillar 3, components III/IV, provenance WHY sources, and the
  AzDO requirement updated; frontmatter added (human-authored, revision noted in
  trajectory). Surgical edits — the author's voice and structure preserved.

**Decisions**

* UA ideas borrowed for Goals 3 & 5 (structure/fingerprints/reviewer-gates, never
  narration); SocratiCode ideas for Goal 4 (ergonomics/distribution, never embedding
  search on the truth path). Rationale in the two analysis docs.
* M3 target recommendation: Fineract (opposite pathology) before InvoiceServices.
* M2 freeze must include the self-ADR register and the escape hatches (reflexive C1–C5);
  v1.0 framed as commitment device, not final correctness.
* Doc placement: README stays at repo root; RUN-REPORT stays in `tests/out/ofbiz-tax/`
  (evidence colocated with artifacts); handoffs live in `spec/planning/`, one per
  completed goal (`Goal<N>-handoff.md`).
* Path convention: repo-relative paths in all docs.

* Repo committed (initial commit, user go-ahead given). `.gitignore` excludes `.env`
  (BYOK key), `.idea/`, `.claude/`, `.venv/`. Root `harness/` mystery resolved: empty
  accidental scaffold, removed. M2 workstream 4's commit item is done early.
* Proving-ground material moved under `tests/` (user decision): `tests/targets/`,
  `tests/out/`, `tests/runs/`, `tests/exit/`. Core product (`pipeline/`, `ir-spec/`,
  `spec/`) stays at root; the plugin architecture survives inside `tests/`. Script
  path derivations and all doc references updated; scripts syntax-checked and path
  resolution verified. Note: OFBiz source was never in this repo — only our plugin
  and evidence.
* Pushed to remote: `git@github.com:DuckMatterLabs/airlift.git` (`origin/main`,
  SSH — HTTPS had no credential helper in this environment).

**Open**
* M3 scope tension flagged: measurement duties (cross-model E1, mutation sweep) live in
  M3 per reflexive C2; user may prefer them elsewhere.
* `fidelity: bug-compatible` field: decide-at-M2 (ship or AADR-defer) — AADR-010.
* Naming: **Airbase and Team Brain are synonyms** — Team Brain is the KB product;
  Airbase is our specialized per-team version of it. Recorded in the whitepaper's
  comparison list and split section, and in the goal-6/goal-7 specs.
* Airlift/Airbase split recorded as normative (goal-7 spec §split): Airlift =
  un-opinionated facts from code+history (semantics + trajectory); Airbase = distilled
  intent; Airlift grounds Airbase and executes its ratified vision. Maestro
  (Symphony-clone PR harness) is the action plane; integration kept thin (MCP + CLIs,
  no Maestro-specific logic in Airlift).

## 2026-07-19 (a) — Goal 1 / M1 completed (prior sessions)

**Done**

* Target-agnostic pipeline + Airlift IR proven end-to-end on Copilot CLI + Opus 4.8
  BYOK against OFBiz `TaxAuthorityServices.rateProductTaxCalc`. E1 35/35 blind, E2 7/7
  mutations named, E3 refactor green, E4 flip caught and named. Evidence:
  `tests/out/ofbiz-tax/RUN-REPORT.md`; state + gotchas: `planning/Goal1-handoff.md`.

**Decisions**

* Pipeline and IR are target-agnostic by user directive (nothing target-specific in
  `pipeline/` or `ir-spec/`).
* Blind repair feedback = compile errors only; red blind test ⇒ fix IR, fresh agent.
* OFBiz H2 loaded seed tiers only — never `loadAll` (demo tax data poisons fixtures).

**Open** (as recorded in Goal1-handoff §6)

* Goals 2–5 unstarted (specs now exist, see entry (b)); airlift repo uncommitted;
  stage-4 external-runner + distill repair-loop debt (→ M2).
