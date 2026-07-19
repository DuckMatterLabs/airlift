---
confidence: proven
related: [ir-spec/IR-SCHEMA.md, spec/adr/README.md, spec/planning/Airlift-m2-freeze.md]
sources: [ir-spec/IR-SCHEMA.md, tests/out/ofbiz-tax/RUN-REPORT.md, spec/analysis/Airlift-reflexive.md]
trajectory: >
  Append-only forever; every schema change lands here as a versioned delta
  (IR-SCHEMA.md section 11) — never as live renegotiation.
---

# Airlift IR schema — changelog

Rules (normative, from `ir-spec/IR-SCHEMA.md` §11): **minor** versions are additive
(new claim-kind, new optional field, new status value) — changelog entry + extended
validators, never weakened. **Major** versions are breaking — they require a migration
tool AND re-proof (the migrated ofbiz-tax IR must pass a fresh-agent E1 rerun; migration
sits on the truth path). A change touching a load-bearing design decision updates or
adds an AADR in `spec/adr/` at merge time (register rule 1).

The current supported version is the single line in `ir-spec/VERSION`;
`pipeline/validate.py` checks every stage artifact's declared `schema_version` against it.

## v1.0 — 2026-07-19 — the freeze

First versioned release. v1.0 is a **commitment device against silent drift, not a
claim of final correctness**. Its own attestation status is `pinned` — it survived one
seam's full proof stack (E1–E4 on OFBiz `rateProductTaxCalc`, evidence:
`tests/out/ofbiz-tax/RUN-REPORT.md`) — not `verified` (multiple pathologies,
cross-model, mutation-swept; that is M3's work).

* Schema text tagged v1.0 (`ir-spec/IR-SCHEMA.md`); frozen-vs-open contract stated
  explicitly in §13: the claim-kind *envelope* is frozen, the claim-kind *set* is open
  (adding a kind is a minor version, by design — AADR-005).
* `schema_version` declaration required on every pipeline stage artifact
  (`fragment-map.yaml`, `coverage-gaps.yaml`, `behavior-catalog.yaml`,
  `backfill-report.yaml`, `ir-manifest.yaml`); all validators version-check (§0).
* Attestation records + deterministic status promotion added (§12):
  `pipeline/promote.py` moves `extracted → pinned → verified` from test evidence;
  forward-only here (demotion is M7's, reading the same records).
* Fragment line-coverage validation anchored to the declared `source_binding.commit`
  (schema §2 principle "lines are evidence, not identity", now enforced as written).

**v1.0 erratum, fixed at tag time**: the schema draft's claim `observability` enum read
`direct | indirect | unobservable-via-contract`, while the validators, the stage-3
prompt, and the proven ofbiz-tax artifacts all use `direct | indirect | internal`.
The schema text was corrected to `internal` (the pinned proof artifacts win over the
draft wording); `validate.py` now enforces the enum on claim files too.

**Deferred delta candidate on record** (decided at M2, not silently dropped):
`fidelity: bug-compatible` claim field — deferred per AADR-010's own falsifier
discipline: no consumer needing the rule-vs-accident distinction exists yet, and
reflexive §3 prohibits speculative schema growth ("walls must be hit, not imagined").
Rationale and design options: `spec/adr/AADR-010-bug-compatible-fidelity.md`. This is
the candidate first minor delta (v1.1).
