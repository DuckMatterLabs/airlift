# Airlift Stage 4 — Backfill: pin uncovered behaviors with code-in-hand tests

You are the Backfill stage of the Airlift distillation pipeline, working inside the target
repository. You HAVE full access to the source (these are code-in-hand tests — distinct from
the later blind, IR-only generation). Inputs (read all first):
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/behavior-catalog.yaml` — the behaviors to pin
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/fragment-map.yaml`
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/targets/ofbiz-tax/harness/harness-contract.md` — the test-harness contract: base class, fixture DSL, claim binding,
  file locations, run command. Follow it exactly.
* The source fragments each behavior traces to.

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

Write integration tests that pin every behavior in the catalog whose coverage is `bare` and
whose observability is `direct`, so the catalog is anchored by executable checks before
distillation.

Rules:
1. One or more test methods per behavior; every test method carries the behavior's claim ID
   via the binding mechanism defined in the harness contract. A test method asserts ONE
   behavior's outcome (plus whatever fixture setup it needs).
2. Use ONLY the fixture DSL from the harness contract for data setup and seam invocation —
   no direct persistence calls, no framework internals. If the DSL cannot express a fixture
   or an assertion you need, STOP working around it; instead record the gap in
   `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/backfill-report.yaml` under `harness_gaps` (these drive harness evolution)
   and skip that behavior.
3. Derive expected values by reading the code (that is the point of code-in-hand tests) —
   exact amounts, exact rounding, exact field values. No tolerance assertions.
4. Behaviors with observability `indirect` or `internal`: do not force a test; record them
   under `untestable` with a reason.
5. Write the test class(es) at the location the harness contract specifies for backfill
   tests. Compile and run them with the harness contract's run command. Iterate until the
   suite is green. If a test fails because the CODE contradicts the catalog (the behavior
   statement is wrong), fix the catalog entry (note it in the report), not the code. Never
   modify production source.

## Output

* Test class(es) at the harness-contract location, compiling and green.
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/backfill-report.yaml`:

```yaml
pinned:
  - claim: <ID>
    tests: [<ClassName#methodName>]
untestable:
  - claim: <ID>
    reason: ...
harness_gaps:
  - need: ...
    blocking: [claim IDs]
catalog_corrections:
  - claim: <ID>
    was: ...
    now: ...
    evidence: <code observation>
run:
  command: <what you ran>
  result: green | red
  notes: ...
```
