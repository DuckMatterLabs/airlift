# Airlift — constitution

Airlift distills business behavior out of entangled legacy code into a typed, **provable**
IR — proven operationally: an agent that has never seen the source generates tests from
the IR alone; green against real code, red against mutations, surviving refactors, naming
violated claims. Goal 1 (M1) is complete; roadmap in `spec/planning/MILESTONES.md`.

## Orientation (read in this order)

1. `spec/index.md` — semantic map of every document.
2. `spec/log.md` — cross-session memory: what happened last, open threads. **Append an
   entry before ending any session that changed anything.**
3. The latest `spec/planning/Goal<N>-handoff.md` (currently `Goal1-handoff.md`) —
   operational state, how to run everything, hard-won gotchas (§4 — do not rediscover
   them). Each completed goal produces its own handoff.

## Standing invariants (do not relax — rationale in spec/planning/MILESTONES.md)

1. **Target-agnostic core.** Nothing target-specific ever lands in `pipeline/` or
   `ir-spec/`; targets are plugins under `targets/<name>/`.
2. **Copilot+Opus executes; Claude authors.** Delivered artifacts must run under the
   `copilot -p` BYOK harness (key in `.env`).
3. **Blind means blind.** IR-only test generation never sees code; repair feedback is
   compile errors only — never test failures or expected-vs-actual.
4. **Red blind test ⇒ fix the IR (or harness), never the test** — then rerun with a
   FRESH agent.
5. **Nondeterminism never sits on the truth path.** LLMs propose; deterministic
   validators and humans ratify. Validator output feeds back verbatim, ≤3 attempts.

## Reflexive discipline (spec/analysis/Airlift-reflexive.md — normative)

Airlift applies its own aspirations to itself: load-bearing design decisions get
self-ADRs with **falsifiers** (what observation would reopen them), tagged load-bearing
vs incidental. Changes touching a load-bearing decision update the AADR register
(`spec/adr/` once M2 materializes it; the seed register is in the reflexive doc §5).
Post-M2, schema changes are versioned deltas — never live renegotiation.

## Frontmatter (required on every generated MD file)

Every MD file authored or generated in this repo carries YAML frontmatter (the
whitepaper's own rule: frontmatter on every note; MD is the log, indexes are
projections). Required fields:

```yaml
---
confidence: proven | analyzed | proposed | speculative   # proven = backed by a green run;
                                                          # analyzed = argued from evidence;
                                                          # proposed = plan, not yet executed;
                                                          # speculative = idea, unvetted
related: [spec/analysis/IR-analysis.md, TAX.EXEMPT.ZEROING]   # concepts/docs/claim IDs this touches
sources: [out/ofbiz-tax/RUN-REPORT.md, spec/drafts/Airlift-IR.md]  # what this was derived FROM
trajectory: >   # a doc is a position; this states its motion — what it feeds,
  Feeds M2 freeze; superseded by spec/adr/ once materialized.   # what supersedes it, when it expires
---
```

Rules: `sources` names inputs (evidence, prior docs, external repos) — empty only for
tier-3 original human prose. `trajectory` must name the downstream consumer or the
condition that retires the doc — "none" is a smell. When editing an existing file that
lacks frontmatter, add it. Existing files are backfilled opportunistically (touched file
⇒ gets frontmatter), not in a bulk pass.

## House rules

* This repo is the user's; **commit only when asked**. The OFBiz clone's `airlift`
  branch (outside this repo) is the mutation-revert baseline — keep its tree clean.
* Repo-relative paths in docs (e.g. `spec/drafts/Airlift-IR.md`), regardless of the
  referencing file's location.
* Evidence stays with its run (`out/<target>/`, `runs/`); specs stay in `spec/`;
  normative schema in `ir-spec/`.
* Python via `./.venv/bin/python`. `pipeline/lib.sh` is bash-only. Long Copilot runs:
  launch detached, watch the log (Claude background tasks die at 10 min).
* Never load OFBiz demo data (`loadAll`) — seed tiers only (Goal1-handoff §2).
