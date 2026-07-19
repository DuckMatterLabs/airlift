---
confidence: proven
related: [ir-spec/CHANGELOG.md, spec/adr/README.md, spec/drafts/Airlift-IR.md, spec/planning/Airlift-m2-freeze.md]
sources: [spec/drafts/Airlift-IR.md, tests/out/ofbiz-tax/RUN-REPORT.md, spec/analysis/Airlift-reflexive.md]
trajectory: >
  The normative contract every pipeline stage and blind generator consumes;
  frozen at v1.0 (2026-07-19). Changes only via the section-11 process,
  recorded in ir-spec/CHANGELOG.md.
---

# Airlift IR — Schema v1.0

The Airlift Intermediate Representation (IR) is a typed stack of claim atoms that captures
business behavior extracted from source code, independent of the architecture that hosts it.
Its sufficiency test is operational, not rhetorical: a subagent that has never seen the source
must be able to generate tests from the IR alone, and those tests must run green against the
live code (and red against mutated code). See `spec/drafts/Airlift-IR.md` for the design rationale.

This document is normative for pipeline stages that emit IR and for the blind test generator
that consumes it. **v1.0 is a commitment device against silent drift, not a claim of final
correctness** — its own attestation status is `pinned` (survived one seam's full proof stack),
not `verified`. What that means and how the schema changes: sections 11–13. Why each frozen
element is the way it is: section 14 (the AADR register).

## 0. Version declaration

The supported schema version is the single line in `ir-spec/VERSION` (currently `1.0`,
`major.minor`). Every pipeline stage artifact — `fragment-map.yaml`, `coverage-gaps.yaml`,
`behavior-catalog.yaml`, `backfill-report.yaml`, `ir-manifest.yaml` — declares the schema
it was written against as a top-level key:

```yaml
schema_version: "1.0"     # quoted string, major.minor
```

Every `pipeline/validate.py` stage checks the declaration: the major version must equal the
supported major; the declared minor must not exceed the supported minor (minor versions are
additive, so older artifacts remain valid under newer validators). A missing or
incompatible declaration is a validation failure. Individual claim files do not carry a
version; the manifest governs its IR directory.

## 1. File layout

```
tests/out/<target>/ir/
  ir-manifest.yaml          # schema_version, package scope, source binding, claim index
  glossary.yaml             # canonical terms (lingua franca; claims may only use these terms)
  domain-model.yaml         # typed domain concepts + attributes + fixture vocabulary
  claims/<CLAIM-ID>.yaml    # one atom per file (progressive disclosure)
  traceability.yaml         # claim <-> fragment-ID map (the code binding)
  provenance/<CLAIM-ID>.yaml  # optional per-claim history evidence (PR-history stub)
  architecture/             # SEPARATE spec: architecture stub (ADRs + pattern library)
    adr/ADR-NNN-*.md
    patterns/*.yaml         # mustache-parameterized pattern snippets
```

## 2. Identifiers

* **Claim IDs**: `<DOMAIN>.<AREA>.<SLUG>` — uppercase, dot-separated, stable forever.
  Example: `TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX`. Renaming is prohibited; supersede instead
  (`superseded_by`).
* **Fragment IDs**: `F-<FILEABBR>-NNN` — a stable handle for a source fragment (a method, a
  block, a query, a condition cluster). Fragments are bound to *symbols and roles*, not line
  numbers; `traceability.yaml` records the file, symbol, extraction-time line range, and the
  git commit at which the range was valid. Line ranges are evidence at that commit, not
  identity — validators that check line coverage do so against the file content *at the
  declared commit*, never against a later working tree.
* **Term IDs**: `term.<kebab-slug>` in `glossary.yaml`.

## 3. Claim atom schema (`claims/<CLAIM-ID>.yaml`)

```yaml
id: TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX     # required, stable
kind: behavior          # behavior | invariant | domain | contract | config
title: One-line statement of the rule          # required
status: extracted       # extracted | pinned | verified — see section 12; moved only by
                        # the deterministic promotion tool, never edited by hand or by
                        # extraction stages (which always write `extracted`)
priority: core          # core | edge | secondary  (test-generation ordering hint)
terms: [tax-adjustment, jurisdiction, tax-exemption]   # every noun MUST resolve to glossary
depends_on: []          # claim IDs this rule presupposes (e.g. rate matching before exemption)
behavior: |             # constrained Gherkin (see section 4) — THE claim body
  Rule: ...
    Given ...
    When ...
    Then ...
decision_table:         # optional; for threshold/priority rules where Gherkin would explode
  columns: [...]
  rows: [...]
scenarios:              # optional but strongly encouraged: concrete numeric examples.
  - name: exempt-party-pays-zero        # These are canonical worked examples with exact
    given: {...}                        # values; the blind generator may transcribe them
    expect: {...}                       # directly into test assertions.
outputs_affected: [adjustment.amount, adjustment.exempt-amount]   # glossary terms
observability: direct   # direct | indirect | internal (honest boundary marker; internal =
                        # not observable through the contract, cannot be blind-tested)
source_fragments: [F-TAS-013, F-TAS-014]   # duplicated in traceability.yaml
confidence: derived-from-code   # derived-from-code | inferred | ratified
notes: >                # optional; WHY-context, oddities, known quirks. Prose allowed here only.
attestations:           # optional; written ONLY by the promotion tool (section 12)
  - tier: verified              # pinned | verified
    evidence: tests/runs/m2-promotion/airliftblind.xml   # the test-results file
    sha: 1a7d91a2…              # target-repo commit the suite ran against
    date: "2026-07-19T11:17:14" # suite run timestamp (from the results file)
```

### Claim kinds

| kind      | meaning                                                        | compiles to |
|-----------|----------------------------------------------------------------|-------------|
| behavior  | business rule: given conditions, observable outcome            | scenario test |
| invariant | property that must hold across all inputs of a class           | property/multi-case test |
| domain    | domain-model fact (entity, attribute, relationship, unit)      | fixture vocabulary + validation |
| contract  | service boundary: inputs, outputs, error modes                 | contract test |
| config    | load-bearing configuration (scales, rounding modes, defaults)  | assertion constants |

The kind *set* is deliberately open — see section 13.

## 4. Constrained Gherkin

Plain Gherkin drifts into prose; these constraints keep it compilable-by-agent:

1. Every noun phrase must be a glossary term (kebab-case, backtick-free). No synonyms.
2. `Given` lines state facts about fixtures in domain vocabulary — never implementation
   identifiers (entity/table names, class or field names, SQL, service names).
3. `When` is always a single canonical action from the contract claims
   (e.g. `When tax is calculated for the order`).
4. `Then` lines assert observable outputs only (terms listed under `outputs_affected`).
5. No `And`-chained hidden algorithms: a complex precondition must be named
   (defined either as its own claim referenced via `depends_on`, or a glossary term).
6. Numbers in `Given`/`Then` are exact decimal strings; rounding semantics cite a
   `config` claim.

## 5. Glossary schema (`glossary.yaml`)

```yaml
terms:
  - id: term.tax-exemption
    canonical: tax-exemption
    definition: >
      A registered status of a purchasing-party with a tax jurisdiction that zeroes the
      tax charged while recording what would have been charged.
    not: ["a zero tax rate", "absence of matching tax rules"]
    aliases: []
```

## 6. Domain model + fixture vocabulary (`domain-model.yaml`)

Describes the *conceptual* entities claims quantify over (jurisdiction, tax-authority,
rate-rule, purchasing-party, store, product, order-line...) with attributes and constraints —
plus, for each, the fixture-builder verbs the test harness exposes (names only; the harness
contract documents signatures). This is what makes blind generation mechanically possible
without leaking architecture.

## 7. Traceability (`traceability.yaml`)

```yaml
source_binding:
  repo: <target repo>
  commit: <sha at extraction>
fragments:
  - id: F-TAS-013                # example from the ofbiz-tax target
    file: <path relative to repo root>
    symbol: handlePartyTaxExempt
    role: business-logic          # business-logic | plumbing | boilerplate | fused
    lines: [729, 771]             # evidence at source_binding.commit, not identity
claims:
  - id: TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX
    fragments: [F-TAS-013, F-TAS-014]
```

## 8. The verification spine

Every generated test method is bound to exactly one claim ID. The binding *mechanism* is
target-owned and defined by the harness contract (the ofbiz-tax target uses an
`@AirliftClaim("<CLAIM-ID>")` annotation — AADR-011); the *requirement* is schema-level.
The spine is the mapping `failing test -> claim ID -> claim title`: when a code change
turns tests red, the report names the violated claims, not just the failing methods.
A claim with zero bound tests is an honest coverage gap and must be listed in
`ir-manifest.yaml` under `unpinned_claims`.

The target's spine reporter must also emit the spine in machine-readable form (a *spine
file*) so target-agnostic tools can consume it without knowing the binding mechanism:

```yaml
suite: airliftblind
results_file: <the JUnit-style XML this spine was derived from>
sha: <target-repo commit the suite ran against>
timestamp: "2026-07-19T11:17:14"
tests:
  - {test: BlindTaxClaimTests#testExemptPartyPaysZero, claim: TAX.EXEMPT.ZEROING, outcome: passed}
unbound: []               # test methods with no claim binding (must be empty for a valid spine)
```

## 9. Architecture stub (separate spec)

Architecture is deliberately excluded from claims: the same claims must survive an
architecture swap. The `architecture/` directory holds the stub needed for grounding:
ADR-format decisions referencing pattern-library entries; patterns are unambiguous
mustache-parameterized snippets (code/config). The test-harness contract is itself a
pattern instance — that is the ONLY architecture the blind generator sees.

## 10. Provenance stub (PR-history)

Where git history clarifies intent, a claim may carry `provenance/<CLAIM-ID>.yaml`:

```yaml
claim: TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX
history:
  - commit: <sha>
    date: 2004-08-01
    summary: exemption recording added alongside zeroing (intent: audit trail)
confidence: inferred    # history testifies; it does not ratify
```

## 11. Versioning & change process

The freeze ends live renegotiation of this schema. Three sanctioned mechanisms, in
increasing severity (from `spec/analysis/Airlift-reflexive.md` §3, normative):

1. **Minor version** (additive): a new claim-kind, a new optional field, a new status
   value. Requires: a `ir-spec/CHANGELOG.md` entry, a `VERSION` bump, and *extended*
   validators — validators are never weakened. Triggered by target friction (M3's
   friction log is the intake queue), never by speculative generality ("Fineract might
   need X" is not a trigger; walls must be hit, not imagined).
2. **Major version** (breaking): field semantics change, kind restructuring. Requires a
   migration tool for existing IR **and** re-proof: the migrated ofbiz-tax IR must pass
   a fresh-agent E1 rerun. Migration is itself a change on the truth path and gets the
   same treatment as any drift.
3. **Basis re-derivation** (the escape hatch, reflexive C3): a scheduled, deliberate
   challenge to the schema's foundations (~M3 and ~M7), not triggered by any incremental
   signal.

Process obligations on every change:

* Changelog entry in `ir-spec/CHANGELOG.md` — no exceptions.
* A change touching a **load-bearing** design decision updates or adds an AADR in
  `spec/adr/` at merge time (register rule 1).
* Schema versions carry their own attestation status (reflexive C5): v1.0 is `pinned`
  (one seam's full proof stack: E1 35/35 blind, E2 7/7 mutations, E3 refactor-green,
  E4 flip named — `tests/out/ofbiz-tax/RUN-REPORT.md`), not `verified` (multiple
  pathologies, cross-model, mutation-swept — that is what M3 buys).

## 12. Statuses, attestations, and promotion

The status ladder is `extracted → pinned → verified`:

| status    | meaning                                                        |
|-----------|----------------------------------------------------------------|
| extracted | asserted by the extraction pipeline; no executable evidence    |
| pinned    | a green code-in-hand (backfill) test is bound to the claim     |
| verified  | a green **blind** (IR-only) test is bound to the claim         |

Statuses are moved ONLY by the deterministic promotion tool `pipeline/promote.py` —
no LLM, no hand edits. Its rules:

* Claim bound to a green backfill test ⇒ at least `pinned`.
* Claim bound to a green blind test ⇒ `verified` (directly, even from `extracted` —
  the ladder orders strength, it does not mandate stepping).
* Any red bound test anywhere in the supplied evidence ⇒ **no promotion** for that
  claim, with the conflict reported.
* Forward-only: promotion never lowers a status. Demotion (staleness, drift) is the
  M7 spine's job and will read the same attestation records.
* Idempotent: re-running on the same inputs is a no-op; a dry-run mode prints the diff.

Every promotion appends an **attestation** to the claim file (shape in section 3):
`{tier, evidence, sha, date}` — the results file, the target-repo commit the suite ran
against, and the suite run timestamp. Attestations are the durable evidence chain;
the manifest's `pinned_claims`/`unpinned_claims` sections remain an extraction-time
index and are not rewritten by promotion. The claim-index `status:` in the manifest is
kept in sync with the claim files.

Inputs to the promotion tool are target-neutral: the machine-readable spine files of
section 8 (which name their results file and commit) plus the IR directory. Extracting
the binding from target test sources stays in the target plugin (AADR-011).

## 13. What is frozen, what is open

**Frozen at v1.0** (breaking any of these is a major version):

* The claim-ID scheme and stability rule (§2), and the fragment-ID binding (§2, §7).
* The claim-kind *envelope*: the atom fields of §3, their semantics, one atom per file.
* The status ladder and attestation record shape (§12), tier semantics included.
* The glossary resolution rule — every claim noun resolves, closed vocabulary (§4.1, §5).
* The constrained-Gherkin grammar for the `behavior` kind (§4).
* The file layout (§1) and the manifest as the IR directory's index.

**Explicitly open** (minor versions, by design):

* The claim-kind *set* (§3): adding a kind — state machine/process, property/invariant
  with generators, richer contract kinds — is a minor version. The envelope was designed
  to carry kinds it does not yet have (AADR-005); v1.0 populating mostly `behavior` is
  an artifact of the first seam, not a boundary.
* New optional atom fields (e.g. the deferred `fidelity:` marker — AADR-010) and new
  status values, provided existing semantics are untouched.
* Directory sharding/namespacing of `claims/` when scale forces it (AADR-003 marks the
  current flatness incidental).

## 14. Design rationale traceability

Every load-bearing element of this schema traces to a decision record in `spec/adr/`
(decision, alternatives considered, evidence, falsifier). The map:

| Schema element | AADR |
|---|---|
| Claim-ID scheme (stable forever, supersede-not-rename) and fragment binding (lines = evidence at commit) (§2, §7) | AADR-012 |
| Constrained Gherkin + decision tables as the claim body (§3, §4) | AADR-001 |
| Closed glossary, mandatory noun resolution (§4.1, §5) | AADR-002 |
| One atom per file, flat YAML, graph as a view (§1, §3) | AADR-003 |
| Operational sufficiency proofs (E1–E4) as the quality gate; status/attestation epistemics (§11, §12) | AADR-004 |
| Open claim-kind stack; envelope frozen, set open (§3, §13) | AADR-005 |
| Freeze itself: co-evolution ended, deltas after (§11) | AADR-006 |
| Author-role vs execution-harness role split | AADR-007 |
| Blind repair feedback = compile errors only (constrains the E1 consumer of this IR) | AADR-008 |
| Per-seam hand-built harness; harness contract as the only visible architecture (§9) | AADR-009 |
| Bug-compatible extraction; `fidelity:` marker deferred (§13) | AADR-010 |
| Spine binding mechanism target-owned; spine requirement schema-level (§8, §12) | AADR-011 |
