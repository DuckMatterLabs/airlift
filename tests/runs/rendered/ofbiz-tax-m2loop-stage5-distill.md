# Airlift Stage 5 — Distill: emit the business-logic IR

You are the Distill stage of the Airlift distillation pipeline, working inside the target
repository. Inputs (read ALL before writing anything):

* `/Volumes/Dancer/Develop/AIRLIFT/airlift/ir-spec/IR-SCHEMA.md` — the normative IR schema. Follow it exactly.
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/fragment-map.yaml`
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/coverage-gaps.yaml`
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/behavior-catalog.yaml`
* `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/backfill-report.yaml` — including catalog corrections discovered while pinning
* `/private/tmp/claude-501/-Volumes-Dancer-Develop-AIRLIFT-airlift/58ea57b7-01f5-469a-b1cf-e02e51cb0dfa/scratchpad/ofbiz-tax-m2loop/harness/harness-contract.md` — the fixture vocabulary your domain model must align with.
* The source fragments themselves (re-read them; precision comes from code, not the catalog).

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

Emit the complete Airlift IR for the seam into `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/ir/`, per the schema:

1. **Every behavior** in the catalog with priority `core` or `edge` becomes a claim atom in
   `claims/<ID>.yaml`. `secondary` behaviors either become claims or are listed in
   `ir-manifest.yaml` under `descoped` with a reason. Behaviors may be merged or split
   relative to the catalog when that yields crisper claims — record any such change in the
   manifest under `catalog_deltas`. Apply the backfill report's `catalog_corrections`.
2. **Constrained Gherkin** per schema section 4. Every noun must be a glossary term; define
   every term you use in `glossary.yaml`. No implementation identifiers anywhere in
   `behavior:` bodies (class, entity, field, service, config-key names are architecture —
   they belong in traceability and the architecture stub, never in claims).
3. **Scenarios with exact numbers**: for every `core` claim provide at least one worked
   numeric scenario (`scenarios:`) whose arithmetic you have verified by hand against the
   code's actual precision/rounding rules. Emit those precision/rounding rules as `config`
   claims (read the actual configuration to get the values; cite files in traceability).
4. **Contract claim(s)**: describe the seam's request/response in domain vocabulary: what a
   caller supplies and what comes back, field by field, in glossary terms. This is what makes
   blind test generation possible.
5. **Domain model** (`domain-model.yaml`): the conceptual entities claims quantify over,
   their attributes (with units/enumerations), and for each concept the fixture verbs from
   the harness contract that construct it. Every `Given` in every claim must be expressible
   via these verbs.
6. **Traceability** (`traceability.yaml`): every claim lists its fragment IDs; every fragment
   carries file/symbol/lines at the current commit. Also ANNOTATE the source: insert a
   comment line (in the file's comment syntax) `AIRLIFT: <fragment-id>` at the first line of
   each business-logic or fused fragment in the primary source file(s) — comment-only change;
   do not alter any code line; the file must still compile.
7. **Provenance**: mine version-control history (`git log --follow`, `git log -S<token>`) for
   the claims the seam brief names as provenance-critical, plus any claim whose intent is
   unclear from code alone: when introduced, what changed semantically since. Emit
   `provenance/<CLAIM-ID>.yaml` per schema. Mark inference honestly (`confidence: inferred`).
8. **Architecture stub** (`architecture/`): ADR-style notes (markdown) for the seam's
   architectural boundaries — at minimum: (a) the public-contract boundary pattern, (b) the
   persistence pattern, (c) the integration-test harness pattern — each referencing a
   mustache-parameterized pattern snippet in `architecture/patterns/`. Keep it a stub: enough
   to show where architecture knowledge lives so claims don't have to carry it.
9. **Manifest** (`ir-manifest.yaml`): top-level `schema_version: "0.9"`
   (required, exactly this value), claim index (id, kind, title, status, priority),
   `pinned_claims` (claims with a green backfill test, from the backfill report),
   `unpinned_claims`, `descoped`, `catalog_deltas`, source binding commit. Every claim's
   `status` is `extracted` — status promotion is a later, deterministic step, never yours.

## Discipline

* Classify, never discard: anything you drop must appear in `descoped` with a reason.
* The IR must stand alone: a reader with the IR and the harness contract — but no access to
  this repository — must be able to write concrete, runnable tests with exact expected
  numbers. Read your own output with that reader's eyes before finishing.
* Claim IDs are forever. Choose them carefully.

Write only under `/Volumes/Dancer/Develop/AIRLIFT/airlift/tests/out/ofbiz-tax-m2loop/ir/` plus the comment annotations in the primary source
file(s).
