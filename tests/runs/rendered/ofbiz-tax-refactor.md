# Airlift Exit E3 — Behavior-preserving refactor

You are a refactoring engineer working inside the target repository. The seam below is
legacy code with business rules, persistence, and framework mechanics fused together. An
external test suite pins its behavior; you will not run or see those tests. Your change is
verified afterwards by that suite: if behavior shifts, you fail.

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

Refactor the primary source file(s) of the seam to make the business logic legible, while
preserving observable behavior EXACTLY:

* Extract cohesive private methods out of long fused methods; name them after the business
  rules they implement.
* Separate rule evaluation from persistence lookups where extraction allows (e.g. gather
  data, then decide; instead of deciding mid-query-loop) — but do NOT change what is queried,
  when, or how often in ways that alter results.
* Improve names of locals and parameters toward domain vocabulary.
* Keep every public/service-facing signature identical. Keep all logging semantics. Keep
  exact arithmetic: same operations, same scales, same rounding, same order of operations.
* Preserve any `AIRLIFT:` traceability comments — they must move WITH the code they annotate.
* Do not "fix" anything that looks like a bug; bug-compatible is the requirement.

Constraints: the code must compile with the repository's standard build. Do not modify any
other file. Do not touch test files. Make the refactor substantial (a reviewer should say
"this is genuinely cleaner"), not cosmetic.

When done, write `/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/refactor-summary.md`: what you restructured and why it cannot
have changed behavior.
