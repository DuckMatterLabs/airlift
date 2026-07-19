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
* Doc placement: README stays at repo root; RUN-REPORT stays in `out/ofbiz-tax/`
  (evidence colocated with artifacts); handoffs live in `spec/planning/`, one per
  completed goal (`Goal<N>-handoff.md`).
* Path convention: repo-relative paths in all docs.

* Repo committed (initial commit, user go-ahead given). `.gitignore` excludes `.env`
  (BYOK key), `.idea/`, `.claude/`, `.venv/`. Root `harness/` mystery resolved: empty
  accidental scaffold, removed. M2 workstream 4's commit item is done early.

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
  `out/ofbiz-tax/RUN-REPORT.md`; state + gotchas: `planning/Goal1-handoff.md`.

**Decisions**

* Pipeline and IR are target-agnostic by user directive (nothing target-specific in
  `pipeline/` or `ir-spec/`).
* Blind repair feedback = compile errors only; red blind test ⇒ fix IR, fresh agent.
* OFBiz H2 loaded seed tiers only — never `loadAll` (demo tax data poisons fixtures).

**Open** (as recorded in Goal1-handoff §6)

* Goals 2–5 unstarted (specs now exist, see entry (b)); airlift repo uncommitted;
  stage-4 external-runner + distill repair-loop debt (→ M2).
