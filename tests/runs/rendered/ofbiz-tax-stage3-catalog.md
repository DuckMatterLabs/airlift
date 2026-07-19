# Airlift Stage 3 — Catalog: enumerate the distinct business behaviors

You are the Catalog stage of the Airlift distillation pipeline, working inside the target
repository. Inputs (read all first):
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/fragment-map.yaml`
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/coverage-gaps.yaml`
* The source itself (re-read the fragments; the map is an index, not a substitute).

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

Enumerate the distinct **business behaviors** present in the seam. A behavior is a rule
a tax accountant or a merchant configuring store taxes could state and care about — not a code path. Granularity guide: one
behavior = one rule statable in a single crisp sentence with observable consequences.

Rules:
1. **Completeness**: every `business-logic` and every `fused` fragment must be represented by
   at least one behavior (or listed under `excluded` with a one-line justification, e.g.
   defensive logging). Plumbing/boilerplate fragments must NOT generate behaviors.
2. Each behavior gets a proposed claim ID `TAX.<AREA>.<SLUG>` — choose short
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

Write exactly one file: `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/behavior-catalog.yaml`

```yaml
areas:
  - area: <AREA>
    meaning: one line
behaviors:
  - id: TAX.<AREA>.<SLUG>
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
