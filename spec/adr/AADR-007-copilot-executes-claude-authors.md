---
confidence: analyzed
related: [spec/adr/AADR-004-operational-sufficiency-proofs.md, spec/adr/AADR-008-compile-errors-only-repair.md, spec/planning/Airlift-m3-second-target.md, CLAUDE.md]
sources: [spec/analysis/Airlift-reflexive.md, spec/planning/Goal1-handoff.md]
trajectory: >
  Softened 2026-07-19 (user directive): the ROLE split is what matters, the
  Claude/Copilot product binding is a deployment detail. Corp deployment will
  run Copilot in both roles; Goal 4/7 may make Airlift itself the harness.
---

# AADR-007 — Two roles: an agent that works ON Airlift, and a harness Airlift runs UNDER

**Tag:** role split load-bearing; the current product binding (Claude authors,
Copilot+Opus executes) incidental. **Status:** active. **Origin:** reflexive D7 /
standing invariant 2, softened per user directive 2026-07-19.

## Decision

Airlift distinguishes two roles, and the distinction — not the products filling
them — is the decision:

* **The authoring role** — the agent that works *on Airlift itself*: pipeline code,
  prompts, validators, specs, harness code, this register.
* **The execution role** — the agent harness that Airlift's artifacts run *under*:
  pipeline stages, blind test generation, the E3 refactor. This role is defined by
  its guarantees, not its vendor: fresh contexts on demand, enforceable sandboxing
  (blindness), prompt-in/artifact-out batch operation.

In the Goal 1 spike the roles were filled by different products — Claude sessions
authored; Copilot CLI (`copilot -p`) + Opus 4.8 BYOK executed — and that split was a
client-environment mandate. The binding is explicitly portable: **one product may
assume both roles** (the corp deployment target has no Claude, so Copilot will author
*and* execute), and in the other direction **Airlift may itself become the harness**
(Goal 4's MCP serving grounded turns, Goal 7's Maestro integration) — at which point
the execution role is filled by Airlift-orchestrated agents rather than a vendor CLI.

What must survive any rebinding: the execution role's *guarantees*. Blindness,
fresh-agent reruns (AADR-008), and determinism-on-the-truth-path are enforced by
process — sandbox recipes, context isolation, validator gates — never by the
accident of author and executor being different vendors.

## Alternatives considered

* **Treat "Copilot executes, Claude authors" as itself load-bearing** — the original
  reading of invariant 2; rejected on reflection: it confuses the spike's staffing
  with the design. Nothing in E1–E4 depends on vendor identity; E1's blindness was
  enforced by `--excluded-tools shell web-fetch` + sandbox cwd (empirically
  verified), which any harness filling the role must replicate.
* **No role distinction at all** (one agent, one context, does everything) —
  rejected: an authoring session that also executes accumulates hidden state on the
  execution path; deliverables must stay runnable by a cold harness from committed
  artifacts alone. This is the part that is genuinely load-bearing, and it holds
  even when one product fills both roles — the roles are separated by *context and
  contract*, not by vendor.

## Evidence

All Goal 1 stages and proofs ran under `copilot -p` (BYOK wrapper `run_copilot` in
`pipeline/lib.sh`) from committed artifacts, with no authoring-session state on the
execution path — demonstrating the roles are separable by contract. Operational cost
of the current execution product is documented (Goal1-handoff §4: detached launches,
bash-only lib, enforced-blindness recipe).

## Falsifier

* **For the load-bearing part (role split):** evidence that role separation by
  context/contract fails when one product fills both roles — e.g., a single-vendor
  deployment where blind runs measurably leak authoring context despite sandbox
  recipes. The corp deployment (Copilot in both roles) is the natural experiment.
* **For the incidental binding:** already expected to change (corp deployment;
  harness/model landscape shifts; Airlift-as-harness at Goals 4/7). Its rebinding is
  routine, not a reopening.
* **Model identity as a separate question:** cross-model E1 (R2) stays scheduled at
  M3. If a different model family generating the blind suite materially changes
  outcomes, *model diversity across the extract/generate boundary* becomes a
  load-bearing requirement of the proof methodology (owned by AADR-004) — desirable
  insurance today, mandate only if the evidence says so. Note the corp deployment
  makes same-model-everywhere the default; the R2 confound must then be managed by
  process (distinct contexts, sandboxing) and periodic cross-model probes where
  available.

## Falsifier observations

* 2026-07-19 — none tripped; cross-model E1 not yet run (scheduled M3). Softening
  recorded: corp target environment has no Claude; Copilot will assume both roles.
