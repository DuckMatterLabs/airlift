# Airlift Stage 2 — Test landscape: what is actually pinned by tests

You are the Test-landscape stage of the Airlift distillation pipeline, working inside the
target repository. Input: `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/fragment-map.yaml` (read it first).

## Target seam

**Repository**: Apache OFBiz — a large Java ERP framework in which business logic is
notoriously entangled with an XML service engine, an entity (persistence) engine, and
multi-language indirection. This working copy is the target codebase.

**Seam under distillation**: sales-tax calculation.

* Primary source file (total fragment coverage required):
  `applications/accounting/src/main/java/org/apache/ofbiz/accounting/tax/TaxAuthorityServices.java`
  — public service entry points `rateProductTaxCalc` and `rateProductTaxCalcForDisplay`, and
  private workhorses `getTaxAuthorities`, `getTaxAdjustments`, `getProductPrice`,
  `setProductCategoryCond`, `handlePartyTaxExempt`.
* External contract: service definitions `calcTax`, `calcTaxForDisplay` and their interfaces
  in `applications/accounting/servicedef/services_tax.xml` — the ONLY wiring of this logic to
  the outside world.
* Persistent-schema definitions the code touches (entity definitions live in
  `applications/datamodel/entitydef/*-entitymodel.xml` and
  `framework/common/entitydef/entitymodel.xml`): TaxAuthority, TaxAuthorityRateProduct,
  TaxAuthorityRateType, TaxAuthorityAssoc, TaxAuthorityGlAccount, TaxAuthorityCategory,
  PartyTaxAuthInfo, PartyRelationship, ProductStore, Product, ProductPrice,
  ProductCategoryMember, PostalAddress, Geo, GeoAssoc, Facility, OrderAdjustment (the output
  shape).
* Load-bearing configuration: `applications/accounting/config/arithmetic.properties`
  (`salestax.calc.decimals`, `salestax.final.decimals`, `salestax.rounding`).
* Helper utilities on the control path (read as needed, no fragment coverage required):
  `GeoWorker.expandGeoRegionDeep`, `ContactMechWorker.getPostalAddressPostalCodeGeoId`,
  `ContactMechWorker.getFacilityContactMechByPurpose`, `ProductWorker.getVariantVirtualId`,
  `ProductWorker.getParentProduct`, `UtilNumber` scale/rounding readers.

**Why this seam**: a single tax calculation resolves taxing jurisdictions from a shipping
address (with geo-region containment), matches rate rules with store/category/threshold
conditions encoded as query predicates, applies party tax exemptions (with party-group
rollup and jurisdiction exemption-inheritance), handles VAT-in-price and price-with-tax
corrections, and splits order-level freight tax across items by value weight — all inline in
one 772-line class, fused with entity-engine query construction. Rule, persistence, and
architectural plumbing are the same tissue.

**Provenance-critical behaviors** (stage 5 must mine history for at least these): the party
tax-exemption rules (exemption zeroing, group rollup, exemption inheritance) and one
threshold rule (minimum item price or minimum purchase).

## Task

Survey the existing automated tests of this repository and determine which behaviors of the
seam (the fragments in the map) are actually covered by executable checks, and which are bare.

1. Derive search tokens from the fragment map (entry-point symbols, contract names, schema
   entity names) and search exhaustively for tests that exercise the seam directly or
   indirectly: unit tests, integration suites, declarative test definitions, scripted tests —
   whatever test idioms this repository uses (discover them; check test registration/config
   files, not just filename conventions).
2. Include indirect coverage: higher-level tests that would invoke the seam as a side effect.
   Read enough of the test and its data to know whether the seam actually fires and whether
   any assertion observes its outcome.
3. For each behavior-carrying fragment (`business-logic` or `fused` in the map), judge:
   `covered-asserted` (a test asserts the outcome), `covered-incidental` (executed by some
   test but outcome not asserted), or `bare` (never executed by any test).
4. Be evidence-based: cite the test file and the assertion line for anything you mark covered.
   Absence claims ("no tests exist") must be backed by the searches you ran.

## Output

Write exactly one file: `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/coverage-gaps.yaml`

```yaml
search_log:            # the searches you ran, so absence claims are auditable
  - pattern: ...
    scope: ...
    hits: N
existing_tests:        # every test artifact found that touches the seam, even indirectly
  - path: ...
    kind: <test idiom>
    touches: [fragment IDs]
    observes_outcome: yes | no
    evidence: file:line of the assertion, or why not
coverage:
  - fragment: F-XXX-007
    status: covered-asserted | covered-incidental | bare
    evidence: ...
summary:
  bare_count: N
  covered_count: N
  headline: one paragraph stating the honest state of test coverage of this seam
```

Do not write any other file. Do not modify source.
