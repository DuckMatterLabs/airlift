---
confidence: proposed
related: [spec/drafts/Airlift-IR.md, spec/analysis/IR-analysis.md, spec/planning/Airlift-m2-freeze.md]
sources: [spec/planning/MILESTONES.md, spec/drafts/Airlift-article.md, spec/drafts/Airlift-IR.md, spec/analysis/IR-analysis.md]
trajectory: >
  Executes as M7 on the M2 promotion tool; feeds M9 productionalization; the
  disposability proof completes the bisimulation thesis; ends in a Goal5-handoff.md.
---

# Airlift Goal 5 spec — Drift detection & compensation (Milestone M7)

Status: NOT STARTED. Date: 2026-07-19.
Prerequisites: **M2** (schema freeze + claim-status promotion tool — the spine's seed).
Composes with M6 (`verify` becomes ambient) but does not require it.
Background: `MILESTONES.md` §M7, `spec/drafts/Airlift-article.md` §Obstacles ("a KB that can be
silently wrong is worse than none"), `spec/drafts/Airlift-IR.md` (round-trip/bisimulation, truth
tiers), `spec/analysis/IR-analysis.md` (Understand-Anything comparison — drift-relevant borrowings).

## Goal summary

The anti-drift spine — the whitepaper's non-negotiable. A stale claim is a
**build-breaking defect, not a documentation chore**. Three capabilities:

1. **Detection** (cheap, ambient, deterministic): notice when bound code changed and when
   IR internal integrity broke.
2. **Adjudication** (executable): decide whether the change *mattered* — run the bound
   claim tests; red names the violated claims (E4, but automatic rather than staged).
3. **Compensation** (the loop): on drift, produce the correct next action — either a
   proposed IR delta (behavior change was intended → versioned claim update for human
   ratification) or a flag on the code (unintended change → the E4 outcome).
   **Distinguishing those two is the whole game.**

Plus the round-trip direction 2 from `spec/drafts/Airlift-IR.md`: regenerate the slice's code from IR
alone and pass the pre-existing suite — bisimulation demonstrated, Java demoted to
projection.

## The two-layer drift model (the UA borrowing, made load-bearing)

Understand-Anything's freshness machinery (per `spec/analysis/IR-analysis.md`) detects *change*
cheaply — content fingerprints per file, re-analyze only what changed — but cannot tell
whether a change *mattered*, because nothing executes. Airlift's spine adjudicates
meaning but is expensive to run wholesale. Compose them:

* **Layer 1 — fingerprints (UA-style, deterministic, runs on every PR/commit):**
  `traceability.yaml` gains a content hash per fragment (normalized source of the bound
  range). A PR recomputes hashes for touched files; changed fragment hash ⇒ affected
  claims identified via traceability — *without running anything*. Unchanged hashes ⇒
  claims attested fresh for free.
* **Layer 2 — spine execution (Airlift-only, runs only on affected claims):** bound
  tests for the affected claims run; green ⇒ re-attest (`verified`, new attestation sha),
  red ⇒ violated claim names out of `report-claims.py`, PR blocked pending explanation.

This gives UA's cost profile (incremental, fingerprint-gated) with Airlift's warrant
(nothing is called "fresh" on similarity grounds; freshness = green test at sha X).

Further UA borrowings, same discipline (structure yes, narration no):

* **Reviewer-gate with verdict** (UA `graph-reviewer`): a deterministic IR integrity
  checker with an explicit pass/fail verdict — every claim's terms resolve in the
  glossary, every `depends_on` resolves, every fragment ID exists in traceability, every
  bound test exists in the suite, no orphan glossary terms, no fragments bound to
  deleted code. Runs in CI on every IR or code change.
* **Freshness metadata + decay**: every claim carries `last_attested: {sha, date,
  result}`; confidence decays with time-since-attestation and hash staleness. Decayed
  claims surface in reports (and in MCP responses, if M6 is live) — never silently.
* **Refused**: UA-style *re-narration* as compensation. Re-extraction output is a
  *proposed delta for ratification*, never an auto-applied update — regeneration without
  ratification is how a self-consistent loop drifts from reality (`spec/drafts/Airlift-IR.md` tier-2
  circularity warning).

## Method

### Phase 1 — CI spine

* PR gate (script-first; CI wiring per target repo): on any PR touching bound fragments —
  Layer 1 hash check → affected claim set → Layer 2 targeted spine run → block on
  unexplained red. Also run the integrity checker unconditionally.
* Claim-status side effects via the M2 promotion tool, now bidirectional: green re-attest
  promotes/confirms; changed-hash-without-green **demotes** `verified → stale` until the
  spine re-greens. `stale` is a first-class status, visible everywhere.
* Coverage recompute: fragments touched by the PR that bind to *no* claim are reported
  ("un-claimed churn") — the honest partial answer to the completeness pitfall
  (IR-analysis pitfall 2); high un-claimed churn is a signal to re-run distillation.

### Phase 2 — compensation loop

On adjudicated drift (red spine or ratified intent change):

* **Code-drift path** (unintended): red report names claims; PR is flagged with the E4
  narrative (claims, scenarios, fragments). The fix is in the code.
* **Behavior-change path** (intended): re-run the distill stage **scoped to the affected
  fragments** (Copilot+Opus, same pipeline, same validators), diff the re-extracted
  claims against stored claims, and open a **proposed IR delta**: versioned claim
  update + changelog entry, tier `inferred` until human ratification. After ratification:
  IR delta → regenerated bound tests → red against old code / green against new →
  provenance recorded (Ledger entry if M4 is live).
* The decision between the two paths is **human-ratified**, evidence-attached — the
  system prepares both narratives; it never guesses intent.

### Phase 3 — round-trip direction 2 (disposability proof)

For at least one slice: a Copilot+Opus agent regenerates the implementation **from IR +
architecture patterns alone** (E1-style blindness — no sight of the original source);
the regenerated code passes the pre-existing behavioral suite (the backfill + blind
suites, and the target's own relevant tests). Mismatches decompose exactly as
`spec/drafts/Airlift-IR.md` predicts: extraction error (fix IR) or dead/buggy code (gold — record
it). This is E1's mirror image and the strongest possible spine attestation: the IR is
demonstrated sufficient not just to *test* the behavior but to *reproduce* it.

## Deliverables

* `pipeline/spine/`: fragment hasher + traceability hash extension (schema delta,
  versioned); integrity checker; PR-gate driver script; staleness/decay reporter.
* Promotion tool extension: demotion, `stale` status, attestation records.
* Compensation-loop runner: scoped re-extraction + claim diff + proposed-delta emitter.
* `exit/run-e7-*.sh` runners for the three exit criteria; drift report (HTML/MD — the
  adoption wedge surface from the whitepaper, pointing humans at drift).

## Exit criteria

1. **Silent-change detection**: a behavior change merged onto a bound fragment with no
   IR update turns CI red **naming the violated claim(s)** — E4 ambient and automatic.
   Verified by replaying the E2/E4 mutations through the PR gate.
2. **Ratified change flows forward**: an IR delta (claim edit) → regenerated bound tests
   red against old code → code change → green, with provenance and status transitions
   recorded end-to-end.
3. **No false-positive tax**: a behavior-preserving refactor (replay E3's diff) passes
   the gate with all hashes changed but all spine runs green — claims re-attested, zero
   human intervention. (This is the discriminating power UA-style file-fingerprinting
   alone cannot have: changed files, unchanged truth.)
4. **Disposability**: regenerated-from-IR code passes the pre-existing suite on at least
   one slice.
5. Integrity checker catches seeded corruption (deleted glossary term, dangling
   `depends_on`, unbound fragment) with exact object names.

## Non-goals

* Auto-ratification. Humans decide intended-vs-unintended; the system only prepares
  evidence. The claim owner's accountability chain (whitepaper §Indemnity) survives
  automation intact.
* Whole-repo drift coverage. The spine covers claim-bound fragments; un-claimed churn is
  *reported*, not adjudicated — expanding claim coverage is distillation work (M3+), not
  spine work.
