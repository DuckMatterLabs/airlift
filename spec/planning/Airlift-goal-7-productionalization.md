---
confidence: proposed
related: [spec/drafts/Airlift-article.md, spec/planning/Airlift-goal-4-mcp.md, spec/planning/Airlift-goal-5-drift.md, spec/planning/Airlift-goal-6-glossary.md, Airbase, Maestro]
sources: [spec/drafts/Airlift-article.md, spec/planning/MILESTONES.md, user directive 2026-07-19 (org context, Airbase/Maestro split)]
trajectory: >
  Executes as M9, last — consumes M2-M8 outputs as stable contracts; absorbs the
  former M8 "KB productization" scope, updated for the Airlift/Airbase split;
  its exit is the whitepaper's adoption thesis tested on one real team.
---

# Airlift Goal 7 spec — Productionalize Airlift (Milestone M9)

Status: NOT STARTED. Date: 2026-07-19.
Prerequisites: M6 (MCP — the serving layer to multi-tenant), M7 (drift spine — the
trust mechanism), M8 (glossary federation — the Airbase bridge). M4's Ledger is the
onboarding carrot's cheapest ingredient. This absorbs the former "M8 — KB
productization" milestone, re-scoped for the org reality below.
Background: `spec/drafts/Airlift-article.md` (components I–IV, adoption, indemnity,
metrics), the user's org directive (2026-07-19): 100+ microservices, ~6 teams, hundreds
of thousands of lines, Conway-partitioned; **Airbase** = our per-team
specialization of the Team Brain KB product (synonyms), distilling intent from
engineers, wikis, PM docs, meetings; **Maestro** = the org's Symphony-clone harness
automating agentic/human turns during PR lifetime.

## Goal summary

Turn Airlift from a proven method into org infrastructure: **centralized persistent
storage and MCP, partitioned per team** (we obey Conway's Law today and design for it
honestly), a defined **responsibility split and cooperation protocol with Airbase**,
**Maestro integration** so claims sit inside the PR lifetime, **proactive drift-fighting
at scale**, and a designed **carrot** — immediate, visible benefit for every team that
onboards, before full distillation pays off.

## The responsibility split (normative — the user's definition, recorded)

* **Airlift is un-opinionated.** It is fact-driven from source code and commit history.
  It captures source-code **semantics** (claims, glossary, architecture patterns) and
  **trajectory** (provenance, the Ledger, drift history). It has **no opinion about
  where the system should go.**
* **Airbase captures intent.** It works with engineers — tribal knowledge, wikis, PM
  docs, ongoing meetings — distilling *what people want and why*. Direction lives there.
* **The relationship**: Airlift is one of Airbase's **grounding sources** — when Airbase
  states intent about existing behavior, it cites Airlift claims (with tiers) rather
  than folklore. And Airlift subsequently **helps execute Airbase's vision of the
  future**: ratified Airbase intent becomes IR deltas (Goal 5's behavior-change path),
  whose regenerated tests and Maestro-driven implementation turn vision into verified
  code. Facts flow up; ratified intent flows down; the diff between Airbase's intended
  behavior and Airlift's attested behavior *is the backlog, made queryable*.
* Consequences for the whitepaper's component map: ingestion pipelines for meetings/
  specs/incidents and the three-tier human-intent discipline land on **Airbase's** side;
  Airlift keeps the code/history side (extraction, spine, Ledger, MCP). The glossary
  (M8) is the shared span. Answer indemnity (clerk-not-oracle, owner on every claim)
  applies on both sides, each citing its own records.

## Architecture

### Store — centralized, partitioned per team

* One central Airlift store; **partition = team** (≈ their services). Inside a
  partition: per-target IRs exactly as today (`tests/out/<target>/ir/` genre), the team's
  glossary namespace, their Ledger slice, their AADR-style decisions.
* Two-layer store discipline (whitepaper §I): MD/YAML files as the event log (git —
  per-team repos or a partitioned monorepo; decide at execution against org git
  topology), derived graph/vector index as a rebuildable projection, never truth.
* **Cross-team read, per-team write.** Ownership is singular per partition (whitepaper
  governance: no shared mush). Cross-team references (a claim depending on another
  team's contract claim) use namespaced IDs via the M8 import mechanism — visible,
  versioned coupling instead of tribal assumption. De-Conway insight stays *reportable*
  (Ledger temporal coupling across partitions) without pretending the org isn't
  partitioned.

### Serving — multi-tenant MCP

* M6's MCP generalized: partition-aware resolution (a Maestro agent working in team A's
  repo gets team A's partition by default, cross-partition on explicit reference),
  same progressive-disclosure layers, same tier envelopes. `.airlift.json` per repo
  names its partition + targets.
* One shared `verify` execution service per partition (spine runs need the team's test
  infrastructure; locks per M6).

### Maestro integration (the action plane)

Maestro automates agentic/human turns across a PR's lifetime. Airlift plugs into the
turns, not beside them:

* **Grounding turn**: Maestro's implementing agents get Airlift MCP as a standard
  grounding source (`claims_for` on the files in scope before writing; the
  "check claims before editing" skill from M6, made default).
* **Gate turn**: the Goal 5 spine runs as a Maestro-orchestrated check — fingerprint
  layer on every PR, targeted spine execution on affected claims; unexplained red
  ⇒ the PR routes to a **human turn with the violated-claim narrative attached**
  (E4's output as a review comment, not a log line).
* **Bookkeeping turn**: on merge, provenance updates (Ledger entry, attestation
  refresh, claim-status transitions) happen inside Maestro's post-merge turn —
  the whitepaper's "work-item transitions generate KB deltas", executed by the harness
  that already owns the PR lifecycle.
* Integration contract kept thin: Maestro consumes the MCP + spine CLIs; Airlift never
  embeds Maestro-specific logic (target-agnostic invariant, extended to harnesses).

## The carrot — designed, not hoped for

Adoption dies when capture-overhead precedes value (whitepaper §Adoption). Onboarding is
therefore **tiered by cost, value-first**:

1. **Day 1 — Ledger only** (deterministic, no LLM, hours not weeks): the team gets
   feature archaeology ("which PRs/tickets shaped this file"), cross-repo temporal
   coupling (their de-Conway report), hotspot maps, onboarding answers. No harness, no
   claims, no behavior-change risk. This is the free sample that creates pull.
2. **Week 1 — hot-seam claims**: Ledger heat ranking picks 1–3 seams; distillation +
   spine on those only. The team's first "CI named the violated claim" moment is the
   conversion event (E4, experienced on *their* code).
3. **Steady state — coverage grows by heat**, never by mandate; drift dashboards and
   stale-claim SLAs keep trust; Airbase starts citing their claims, which makes the
   team's facts *load-bearing in org conversations* — the social carrot.

Measured by the whitepaper's metrics table (time-to-answer, onboarding time, PR rework
from misunderstood behavior, drift count, agent task success); baseline captured at
onboarding so the delta is real, not vibes.

## Drift at scale (proactive)

* Goal 5's spine per partition, in CI and in Maestro's gate turn — drift caught at PR
  time, not discovered at read time.
* Freshness dashboards per partition (decayed attestations, un-claimed churn, stale
  terms); a **stale-claim SLA** owned by the partition owner — a stale claim is a
  build-breaking defect (whitepaper's non-negotiable), and at org scale that needs an
  owner and a clock, not a principle.
* Quarterly reflexive review per partition (mini basis-re-derivation: does the claim
  set still match where the team's churn actually is — Ledger heat vs claim coverage).

## Deliverables

* Partitioned central store (topology decision + migration of existing targets into
  partition #1); multi-tenant MCP deployment.
* Maestro integration: grounding + gate + bookkeeping turns, on one pilot repo.
* Onboarding kit: tier-1 Ledger onboarding automated (point at repos, get reports);
  tier-2 playbook (seam selection by heat, harness scaffold checklist from M3's cost
  learnings); metrics baseline capture.
* Airbase cooperation protocol doc (grounding citations format, intent→IR-delta path,
  glossary bridge ops) — co-owned with the Airbase side.
* Governance pack: partition ownership, SLA, liability-chain / system-of-record
  mandate text for leadership sign-off (the indemnity design needs the mandate to work).

## Exit criteria

1. **The whitepaper's loop, live**: one real team runs one real feature through it —
   Airbase intent doc cites Airlift claims; ratified delta → IR change → red generated
   test → Maestro-driven implementation → green → provenance and ticket update
   themselves. Every tier boundary visible in the artifacts.
2. **Partition proof**: ≥2 teams' partitions live in the central store; cross-team
   claim reference resolves via namespaced import; write isolation enforced.
3. **Maestro gate proof**: a PR with an unintended behavior change on a bound fragment
   is blocked in Maestro's flow with the violated claim named in the review surface;
   a behavior-preserving PR passes with zero human intervention (the E3/E4 pair,
   ambient, on org infrastructure).
4. **Carrot proof**: a newly onboarded team gets tier-1 value (Ledger reports) within
   one day of onboarding start, measured; their baseline metrics captured.
5. **The adoption bet, measured**: on the pilot, asking Airlift beats asking a colleague
   on time-to-answer, with the answer's paper trail (claim, tier, owner) intact.

## Non-goals

* Building or governing Airbase — cooperation protocol only; intent capture is theirs.
* Un-Conway-ing the org. Partitions mirror teams on purpose; the Ledger *reports*
  cross-team coupling, humans decide what to do about it.
* Whole-org distillation coverage. Claims grow by heat where they pay; the Ledger
  covers everything cheaply; 100% claim coverage is explicitly not the target.
* Replacing Maestro's orchestration or the org's CI — Airlift provides grounding and
  gates; it does not own the pipeline.
