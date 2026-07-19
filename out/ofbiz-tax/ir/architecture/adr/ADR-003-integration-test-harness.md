# ADR-003: Integration-test harness pattern — seeded fixtures + service invocation + claim binding

Status: descriptive (the harness contract is the single architecture instance the blind
generator is allowed to see)

## Context

Because both persistence and rule-matching run inside the entity/service engines, the seam
cannot be unit-tested in isolation without heavy mocking. The chosen approach (documented in
`targets/ofbiz-tax/harness/harness-contract.md`) is an **integration harness** that runs against
a real (test-scoped) delegator/dispatcher:

- A fixture DSL (`fx.calcTax()...` builder and its `fx.*` fixture verbs) seeds the entity rows a
  scenario needs (store, authorities, rate rules, party registrations, product prices, postal
  address + geos, facility).
- The builder's `.run()` invokes the `calcTax` service through the dispatcher and returns a
  typed view over the resulting adjustments (`.type()`, `.amount()`, `.includedTax()`,
  `.exemptAmount()`, `.ratePercent()`, `.jurisdictionGeo()`, `.customerTaxId()`); the display
  path returns `taxTotal` / `taxPercentage` / `priceWithTax`.
- Monetary assertions use `assertDecimal` at the precision the config claims specify.

## Decision / boundary

Each generated test method binds to exactly one claim via `@AirliftClaim("<CLAIM-ID>")` (the
verification spine, schema §8). A red test therefore names a violated claim, not just a method.
The `given:` blocks of every claim are expressed only through fixture verbs enumerated in
domain-model.yaml, so a blind generator can transcribe a claim's `scenarios:` directly into a
seeded-fixture test without repository access.

## Consequences

The harness contract is a *pattern instance*: it is the only architecture the generator sees,
and it is parameterized in `architecture/patterns/integration-test.mustache`. Anything the
harness cannot express is an honest coverage boundary (claims marked
`observability: internal`, e.g. TAX.VAT.PRICE-GROUP-SCOPE, TAX.ORDER.VALUE-WEIGHT).

## Pattern

See `architecture/patterns/integration-test.mustache`.
