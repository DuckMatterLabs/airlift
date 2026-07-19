---
confidence: proposed
related: [spec/drafts/Arlift-PR-history-distillation.md, spec/planning/MILESTONES.md, spec/planning/Airlift-m2-freeze.md]
sources: [spec/drafts/Arlift-PR-history-distillation.md, spec/planning/MILESTONES.md, spec/drafts/Airlift-IR.md, spec/planning/Goal1-handoff.md]
trajectory: >
  Executes as M4 after M2 freeze; Ledger feeds Goal 5 provenance recording and
  Goal 3 ADR intent-mining; ends in a Goal2-handoff.md.
---

# Airlift Goal 2 spec — PR-history distillation (Milestone M4)

Status: NOT STARTED. Date: 2026-07-19.
Prerequisite: **M2 (schema freeze)** — this goal consumes the claim schema as a fixed,
versioned contract. Any IR gap discovered here ships as a versioned schema delta, never a
live renegotiation.
Background: `spec/drafts/Arlift-PR-history-distillation.md` (discussion + prior art),
`MILESTONES.md` §M4, `spec/drafts/Airlift-IR.md` (truth tiers), `spec/planning/Goal1-handoff.md` §6 thread 1.
Execution harness: Copilot CLI (`copilot -p`) + Opus BYOK for all LLM phases (Phase 2);
Phase 1 is deterministic and uses no LLM at all.

## Goal summary

Deconstruct git's cumulative projection into layers — *multilevel blame* — joined across
repos via the work-item hierarchy, and use it to answer WHY for claims. Two regimes,
strictly separated:

* **Index eagerly** (Phase 1, one-time, deterministic, no LLM): the **Ledger**.
* **Extract lazily** (Phase 2, on-demand, LLM, human-ratified): claim-driven mining,
  backwards from a claim, never forward from history.

History is deposed as a witness, not transcribed as scripture: it testifies, a human
ratifies. The output of mining is always a **tier-3 inferred claim** awaiting ratification
— never a silent promotion.

## Inputs (fixed contracts)

* `ir-spec/IR-SCHEMA.md` v1.0 — frozen. Claims, `traceability.yaml`, fragment IDs
  (`F-TAS-NNN`), and the `provenance/*.yaml` stubs emitted by stage 5 (git-only today) are
  the seed this goal grows from.
* `tests/out/ofbiz-tax/ir/` — 34 claims with fragment bindings; the mining demos run here.
* Target git repos. For ofbiz-tax: the OFBiz clone (`airlift` branch for harness, but
  history mining runs over upstream history). OFBiz work items are Apache JIRA keys
  (`OFBIZ-NNNN`) embedded in commit messages — no PR/ticket API needed for the first
  target, which keeps Phase 1 honest about its adapter design (see below).

## Method

### Phase 1 — the Ledger (deterministic, no LLM, rebuildable)

A SQLite index of pointers — **never diff contents** (git already stores every delta; the
Ledger stores the *join*):

```
repo(id, url)
commit(sha, repo, author, authored_at, message)
pr(id, repo, title, description, author, merged_at, merge_commit)
pr_commit(pr, sha)
file_touch(sha, path, change_kind, old_path)        -- rename-aware
symbol_touch(sha, path, symbol, change_kind)         -- best-effort, language adapter
work_item(id, kind, title, parent)                   -- Story → Feature → Epic chain
commit_work_item(sha, work_item, evidence)           -- msg pattern, PR link, branch name
fragment_binding(fragment_id, path, symbol, since_sha)  -- joins Ledger to IR traceability
incident_link(work_item | pr, incident_id)
```

Design rules:

* **Target-agnostic core, forge adapters as plugins.** The walker and schema live in
  `pipeline/`; the *work-item resolver* (JIRA-key regex for OFBiz, AzDO API for Engage,
  GitHub PR API) is a `tests/targets/<name>/ledger/` adapter. Same invariant as everywhere else:
  nothing forge-specific in the core.
* **Stable IDs across renames.** Files/methods rename and die; follow renames via git's
  rename detection and record `old_path` chains. Where symbol-level tracking is needed,
  borrow mechanics from CodeShovel/CodeTracker (clones available under
  `/Volumes/Dancer/Develop/AIRLIFT/`) rather than inventing method-tracking. Fragment IDs
  (`F-TAS-NNN` source annotations) are the IR-side stable anchor; the Ledger's job is to
  map history onto them.
* **Validate metrics against Code Maat.** Churn-per-file/symbol and temporal coupling are
  computed over Ledger tables; run Code Maat on the same log and require agreement (within
  documented tolerance) before trusting our numbers. Do not adopt Code Maat as the store.
* Rebuildable from scratch at any time; building it twice yields identical tables
  (deterministic ordering, no timestamps of the build itself in the data).

### Phase 2 — claim-driven mining (on-demand, LLM, ratified)

For a claim needing provenance/WHY, run archaeology **backwards from the claim**:

1. Claim → `traceability.yaml` → fragment IDs → bound source ranges.
2. `git blame` (SZZ-style walk: follow modified/deleted lines through history) → the
   commits that shaped the fragment.
3. Ledger joins: commits → PRs → work items → sibling PRs on the same Feature/Epic
   (the cross-repo, de-Conway'd context).
4. LLM synthesis over that **bounded bundle only** (diffs of those commits + PR/ticket
   text): a provenance narrative with explicit citations — "introduced in `sha`/PR#n under
   OFBIZ-nnnn; changed in PR#m with rationale X."
5. Output lands in `tests/out/<target>/ir/provenance/<claim>.yaml` as **tier-3 inferred**,
   `status: inferred`, `awaiting: ratification`, with every assertion carrying a
   commit/PR/ticket citation. A deterministic validator checks that every cited sha/PR/
   work-item actually exists in the Ledger and actually touches the claim's fragments —
   the anti-hallucination gate.

Repair loop as in the M1 pipeline: validator output feeds back verbatim, ≤3 attempts.

### Phase 3 — heat prioritization

Never mine uniformly. The Ledger gives churn-per-symbol and incident correlation for
free; mine hotspots first: **high churn × incident-linked × claim-bound**. Cold history
stays unmined forever; the Ledger holds the pointers if anyone asks. Emit a ranked
`mining-queue.yaml` so the ordering itself is an inspectable artifact.

## Deliverables

* `pipeline/ledger/` — schema, walker, metric queries (churn, coupling, knowledge map);
  `tests/targets/ofbiz-tax/ledger/` — JIRA-key adapter.
* `pipeline/prompts/mine-claim.md` (parameterized, target-agnostic) + `validate.py` stage
  for provenance narratives.
* Provenance narratives for ≥5 claims from the M1 IR, ratification-ready.
* `mining-queue.yaml` heat ranking for the ofbiz-tax seam.

## Exit criteria

1. The Ledger answers the four required questions **deterministically** (SQL, no LLM):
   which features touched this fragment; when was this functionality first bound to which
   feature; which repos a feature touched; how behavior changed by feature (as the list of
   claim-bound fragment touches per feature).
2. For **≥5 claims** from M1's IR, mining produces provenance narratives whose every
   commit/PR/ticket citation passes the deterministic citation validator; all land as
   tier-3 inferred, none silently promoted.
3. Ledger churn/coupling numbers reconciled against Code Maat on the same history.
4. Any IR schema gap discovered (e.g., a `provenance` field the narrative needs) ships as
   a versioned schema delta with changelog entry.

## Invariants and non-goals

* Standing invariants from `MILESTONES.md` apply (target-agnostic core; Copilot+Opus
  executes; nondeterminism never on the truth path — here: the Ledger and the citation
  validator are the deterministic truth path; the LLM only writes narrative *over* it).
* Non-goal: bulk summarization of history ("extract knowledge once and for all" is the
  trap the background spec names). No narrative is generated without a claim asking.
* Non-goal: WHY is reconstruction, not fact. Mined narratives explain *what changed and
  when* with certainty and *why* with humility; the tier system carries that distinction.
