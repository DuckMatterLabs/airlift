---
confidence: analyzed
related: [spec/drafts/Airlift-article.md, ir-spec/IR-SCHEMA.md, spec/planning/MILESTONES.md]
sources: [spec/drafts/Airlift-article.md, spec/analysis/IR-analysis.md, tests/out/ofbiz-tax/RUN-REPORT.md, tests/runs/m2-freeze-report.md]
trajectory: >
  The human-facing front door. Updated at milestone boundaries; the manifesto
  (spec/drafts/Airlift-article.md) stays the founding vision, this projects it
  plus the earned evidence. Superseded section by section as milestones land.
---

# Airlift

**Airlift distills the business behavior buried in legacy code into a typed, provable
knowledge base — and proves it the hard way: an AI agent that has never seen the source
generates tests from the knowledge base alone, and those tests run green against the
real system and red against sabotaged copies of it.**

Not a wiki. Not documentation. A system of record for *what the code actually does*,
with an evidence trail for every claim it makes.

## The problem: code is a position, not a trajectory

Source code is the ultimate snapshot of a system — and only a snapshot. The system is
more than that: it has a history and a direction. Why is the threshold `>=` and not
`>`? Which behaviors are load-bearing rules and which are accidents nobody dared touch?
The WHYs evaporate; what remains is archaeology.

This was always painful for humans. For AI agents it is fatal in a quieter way: an
agent grounded on raw code reconstructs intent by guessing — fluently, plausibly, and
*differently on every run*. Agents writing new code from existing code is photocopying
a photocopy; the quality degrades generation by generation.

"So ground the agent with architecture and PM docs." Two problems: **the gap** and
**the rot**. The PM doc speaks intent ("users shouldn't be notified twice"); the code
speaks mechanism. Between them there is no shared vocabulary, no links, no intermediate
structure — the agent bridges the gap by guessing again. And prose docs drift worse
than code, because nothing executes them and nothing fails when they go stale. A
knowledge base that can be silently wrong is worse than none: the first time someone
gets burned, they stop trusting it, and a distrusted KB is dead.

## The bet: claims you can falsify, not prose you must believe

Airlift extracts behavior into an **intermediate representation (IR)** of typed claim
atoms — small YAML files, one claim each, with stable IDs:

```yaml
id: TAX.EXEMPT.ZEROING
kind: behavior            # behavior | invariant | domain | contract | config
title: A valid exemption zeroes the tax and records the would-be tax as an exempt amount.
status: verified          # extracted -> pinned -> verified, moved only by evidence
behavior: |               # constrained Gherkin over a closed glossary — no synonyms,
  Rule: ...               # no implementation names, exact decimal arithmetic
scenarios: [...]          # worked numeric examples an agent can transcribe into asserts
source_fragments: [F-TAS-055]   # binds the claim to code, at a pinned commit
attestations:             # the evidence: which suite, which commit, when
  - {tier: verified, evidence: tests/runs/m2-promotion/airliftblind.xml, sha: 1a7d91a2…, date: ...}
```

Three design commitments make this different from documentation:

* **A closed glossary as lingua franca.** Every noun in every claim must resolve to a
  defined term. No synonyms, no colloquial drift — the agent never bridges a
  vocabulary gap by guessing, because there is no gap.
* **Determinism on the truth path.** LLMs propose; deterministic validators and humans
  ratify. Claim statuses (`extracted → pinned → verified`) are moved only by a
  deterministic tool reading test results — never by a model, never by hand.
* **Architecture is quarantined.** Claims speak business vocabulary only; the
  architecture lives in a separate stub. The same claims must survive an architecture
  swap — that is what makes them *knowledge* rather than a paraphrase of the code.

The sufficiency test is operational, not rhetorical. Four exit criteria, all executed
against Apache OFBiz's tax-calculation seam (~800 lines of deeply entangled
seventeen-year-old Java):

| Proof | What it demonstrates | Result |
|---|---|---|
| **E1 — blind generation** | A sandboxed agent (no shell, no web, no source access) writes tests from the IR alone; they must pass against the real code | **35/35 green** |
| **E2 — mutation kill** | 7 realistic planted bugs; the blind suite must catch them and *name the violated claim* | **7/7 caught & named** |
| **E3 — refactor survival** | A 675-line behavior-preserving refactor; the suite must stay green (claims outlive structure) | **35/35 green** |
| **E4 — semantic flip** | Flip one business rule; the suite must go red and name exactly the right claims | **4 TAX.EXEMPT claims named** |

The first blind run scored 30/35 — and that is the method working, not failing: the
five reds exposed three genuinely ambiguous claims and a harness bug. The *claims* were
repaired (never the tests), and a fresh agent then hit 35/35. The blind agent is the
forcing function that makes the IR earn its precision.

Since the M2 freeze, the schema is a versioned contract (v1.0) and every claim's status
carries sha-stamped attestations: of 34 claims, 27 are `verified` (blind-test-proven),
3 `pinned` (code-in-hand tests), 4 honestly `extracted` — the gaps are visible instead
of papered over. *Don't trust what you haven't tried to falsify* — Airlift applies that
to itself, too: every load-bearing design decision is recorded with a **falsifier**
(the observation that would reopen it) in `spec/adr/`.

## Relation to Code Digital Twin: the genus and a species

Peng & Wang's [Code Digital Twin](https://arxiv.org/abs/2503.07967) independently
arrives at the same diagnosis (tacit knowledge scattered spatially, tangled temporally;
task-time context engineering recovers *what* but not *why*) and the same architectural
move: a persistent, curated knowledge layer atop the codebase. CDT is the genus;
Airlift is a species that makes three bets the genus doesn't:

1. **Provable, not merely traceable.** CDT's ceiling is traceability — every claim
   links to evidence, but nothing *executes* the twin's knowledge, so a wrong rationale
   survives until a human notices. Airlift's claims are warranted by blind tests:
   mutation-red, refactor-green. CDT validates usefulness; Airlift validates truth.
2. **Inversion, not a mirror.** A twin reflects and co-evolves, permanently subordinate
   to the code. Airlift explicitly intends to *invert the source of truth* — demote the
   codebase to a secondary artifact to unlock free refactoring and truly agentic
   feature work. More ambitious, riskier, and the reason provability is non-negotiable.
3. **Facts and intent kept apart.** CDT folds commits, mailing lists, and design
   threads into one twin with one confidence economy. Airlift is deliberately the
   **fact half** of a two-KB system: it testifies to what the code provably does and
   how it got there; its sibling (**Airbase**) captures what people *intend*, bridged
   by the shared glossary. The split exists because intent has no test suite — and
   blurring the two tiers once burns the trust both need.

## Why the evidence trail matters: the clerk, not the oracle

The deepest adoption blocker for any AI knowledge system isn't accuracy — it's
liability. Asking a colleague isn't just information retrieval; it transfers
responsibility. An AI's answer transfers none, so a 99%-accurate KB loses to a
90%-accurate human. Organizations solved this long ago for payroll and the general
ledger: **systems of record**, where citing the system *is* the due diligence.

That is what the attestation machinery is for. Airlift never answers as an oracle
("the tax is zeroed") — it answers as a clerk reading a signed record ("per claim
TAX.EXEMPT.ZEROING, `verified`: blind-tested green against commit `1a7d91a2` on
2026-07-19"). Tiered honestly: a verified claim, a code-derived claim, and an AI
inference must never wear the same confidence. Answers arrive with a paper trail so
that relying on them is defensible — trust designed to be *auditable*, so the
double-check rate can decay with track record.

## How it works

A target-agnostic five-stage pipeline (currently executed by GitHub Copilot CLI +
Claude Opus 4.8), where every stage's output must survive a deterministic validator —
failures feed a bounded repair loop, and nothing nondeterministic sits on the truth
path:

```
map ──▶ testscape ──▶ catalog ──▶ backfill ──▶ distill ──▶ [blind testgen ⇒ E1–E4 proofs]
 inventory   what tests    distinct     pin bare      emit claims,
 + classify  really pin    behaviors    behaviors     glossary, traceability
 every line  (evidence-    a domain     with code-    — then prove it blind
 (fused =    based)        expert       in-hand
 the hard                  would name   tests
 targets)
```

Targets are plugins (`tests/targets/<name>/`): a descriptor, a seam brief, a test
harness with a fixture DSL, and a mutation kit. The pipeline and schema contain zero
target specifics — that separation is an enforced invariant, not an aspiration.

## Status and roadmap

| Milestone | State |
|---|---|
| **M1** — pipeline + IR proven end-to-end on one seam (E1–E4) | ✅ complete |
| **M2** — schema frozen at v1.0; versioned change process; deterministic status promotion; self-ADR register with falsifiers | ✅ complete |
| **M3** — second target: real production code; the measurement milestone (cross-model blind generation, full mutation sweep, cost accounting) | next |
| M4–M9 — PR-history mining (the Ledger), architecture distillation, MCP serving layer, drift spine, glossary federation, org rollout | specified in `spec/planning/` |

v1.0's own attestation status is `pinned`, not `verified` — it survived one seam's full
proof stack, and M3 exists to try to break it on a second pathology.

## Reading order

* **The vision:** `spec/drafts/Airlift-article.md` — the founding manifesto (two-KB
  split, glossary, indemnity, adoption).
* **The IR, and why this shape:** `spec/drafts/Airlift-IR.md` (rationale),
  `ir-spec/IR-SCHEMA.md` (the frozen v1.0 contract).
* **The skeptic's tour:** `spec/analysis/IR-analysis.md` (pitfalls + the CDT
  comparison) and `spec/analysis/Airlift-reflexive.md` (Airlift's own design decisions
  with falsifiers — the project critiquing itself).
* **The receipts:** `tests/out/ofbiz-tax/RUN-REPORT.md` (M1 proof-run narrative),
  `tests/runs/m2-freeze-report.md` (M2 execution evidence).
* **Working on the repo** (humans and agents): start at `AGENTS.md` / `CLAUDE.md`,
  then `spec/planning/Goal1-handoff.md` — §3 to run everything, §8 for current
  operational discipline. The Goal-1 component map lives in
  `tests/runs/goal1-spike-README.md`.
