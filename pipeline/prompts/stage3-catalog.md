# Airlift Stage 3 — Catalog: enumerate the distinct business behaviors

You are the Catalog stage of the Airlift distillation pipeline, working inside the target
repository. Inputs (read all first):
* `{{OUT_DIR}}/fragment-map.yaml`
* `{{OUT_DIR}}/coverage-gaps.yaml`
* The source itself (re-read the fragments; the map is an index, not a substitute).

## Target seam

{{SEAM_BRIEF}}

## Task

Enumerate the distinct **business behaviors** present in the seam. A behavior is a rule
{{DOMAIN_EXPERT_PERSONA}} could state and care about — not a code path. Granularity guide: one
behavior = one rule statable in a single crisp sentence with observable consequences.

Rules:
1. **Completeness**: every `business-logic` and every `fused` fragment must be represented by
   at least one behavior (or listed under `excluded` with a one-line justification, e.g.
   defensive logging). Plumbing/boilerplate fragments must NOT generate behaviors.
2. Each behavior gets a proposed claim ID `{{CLAIM_NAMESPACE}}.<AREA>.<SLUG>` — choose short
   uppercase AREA tokens that partition the domain naturally, and record the area list with
   one-line definitions in an `areas` section.
3. State each behavior as one crisp sentence in business vocabulary — no implementation
   identifiers (class, entity, field, service names). Note its inputs (the facts it depends
   on) and its observable outcome through the seam's public contract (what a caller can see).
4. Mark `observability`: `direct` (visible in the contract's outputs), `indirect` (visible
   only via side effects), `internal` (not observable through the contract — be honest;
   these cannot be blind-tested and must say so).
5. Carry over coverage status from stage 2 (`bare` / `covered-*`).
6. `priority`: `core` (the rule is the point of the seam), `edge` (conditional refinements),
   `secondary` (auxiliary annotations, display variants).

## Output

Write exactly one file: `{{OUT_DIR}}/behavior-catalog.yaml`

```yaml
schema_version: "{{SCHEMA_VERSION}}"   # required top-level key, exactly this value
areas:
  - area: <AREA>
    meaning: one line
behaviors:
  - id: {{CLAIM_NAMESPACE}}.<AREA>.<SLUG>
    statement: one sentence, business vocabulary
    inputs: [facts it depends on]
    observable_outcome: what the caller sees
    observability: direct | indirect | internal
    fragments: [F-XXX-013]
    coverage: bare
    priority: core
excluded:
  - fragment: F-XXX-002
    reason: ...
```

Do not write any other file. Do not modify source.
