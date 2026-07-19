# Airlift IR — Schema v0.1

The Airlift Intermediate Representation (IR) is a typed stack of claim atoms that captures
business behavior extracted from source code, independent of the architecture that hosts it.
Its sufficiency test is operational, not rhetorical: a subagent that has never seen the source
must be able to generate tests from the IR alone, and those tests must run green against the
live code (and red against mutated code). See `spec/drafts/Airlift-IR.md` for the design rationale.

This document is normative for pipeline stages that emit IR and for the blind test generator
that consumes it.

## 1. File layout

```
out/ir/
  ir-manifest.yaml          # package scope, source binding, claim index
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
  git commit at which the range was valid. Line ranges are evidence, not identity.
* **Term IDs**: `term.<kebab-slug>` in `glossary.ya
* ml`.

## 3. Claim atom schema (`claims/<CLAIM-ID>.yaml`)

```yaml
id: TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX     # required, stable
kind: behavior          # behavior | invariant | domain | contract | config
title: One-line statement of the rule          # required
status: extracted       # extracted -> pinned (a green code-in-hand test exists) -> verified (blind test green)
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
observability: direct   # direct | indirect | unobservable-via-contract (honest boundary marker)
source_fragments: [F-TAS-013, F-TAS-014]   # duplicated in traceability.yaml
confidence: derived-from-code   # derived-from-code | inferred | ratified
notes: >                # optional; WHY-context, oddities, known quirks. Prose allowed here only.
```

### Claim kinds

| kind      | meaning                                                        | compiles to |
|-----------|----------------------------------------------------------------|-------------|
| behavior  | business rule: given conditions, observable outcome            | scenario test |
| invariant | property that must hold across all inputs of a class           | property/multi-case test |
| domain    | domain-model fact (entity, attribute, relationship, unit)      | fixture vocabulary + validation |
| contract  | service boundary: inputs, outputs, error modes                 | contract test |
| config    | load-bearing configuration (scales, rounding modes, defaults)  | assertion constants |

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

Every generated test method is bound to exactly one claim ID (mechanism defined by the
harness contract: `@AirliftClaim("<CLAIM-ID>")` annotation). The spine is the mapping
`failing test -> claim ID -> claim title`: when a code change turns tests red, the report
names the violated claims, not just the failing methods. A claim with zero bound tests is
an honest coverage gap and must be listed in `ir-manifest.yaml` under `unpinned_claims`.

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
