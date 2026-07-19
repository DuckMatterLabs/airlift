# Sales-Tax Seam Refactor — TaxAuthorityServices

**File:** `applications/accounting/src/main/java/org/apache/ofbiz/accounting/tax/TaxAuthorityServices.java`

Goal: make the tax-calculation business logic legible by extracting cohesive, business-named
private methods out of the ~300-line `getTaxAdjustments` monolith (and the VAT block of
`rateProductTaxCalcForDisplay`), while preserving observable behavior **exactly**.

## What was restructured

All public/service-facing signatures are **unchanged**: `rateProductTaxCalc` and
`rateProductTaxCalcForDisplay` keep identical signatures, so the `calcTax` / `calcTaxForDisplay`
service wiring in `services_tax.xml` is untouched. The private overloads `getTaxAdjustments`,
`getProductPrice`, `setProductCategoryCond`, `handlePartyTaxExempt`, and `getTaxAuthorities`
also keep their signatures.

New private helpers extracted (each named for the business rule it implements):

| New method | Extracted from | Responsibility |
|---|---|---|
| `resolveVatTaxAuthorities` | `rateProductTaxCalcForDisplay` (F-TAS-007) | Resolve store VAT tax authorities (party-specific vs geo-wide). |
| `buildProductStoreCondition` | `getTaxAdjustments` (F-TAS-031) | Store-scoped-or-null rate-rule predicate. |
| `buildTaxAuthoritiesCondition` | `getTaxAdjustments` (F-TAS-032) | `_NA_` + per-authority (geo/party) OR predicate. |
| `buildRateRuleLookupCondition` | `getTaxAdjustments` (F-TAS-036) | Combine store/authority/category with the `minItemPrice` and `minPurchase` **threshold rules**. |
| `buildTaxAdjustmentsForRateRule` | `getTaxAdjustments` per-rule loop body (F-TAS-038..046) | Apply one matched rate rule and emit its OrderAdjustment(s). |
| `computeTaxableAmount` | rule loop (F-TAS-038) | Sum taxable base (item + taxable shipping + taxable promotions). |
| `resolveTaxAuthGlAccountId` | rule loop | Look up GL account by pay-to party. |
| `resolveTaxProductPrice` | rule loop (F-TAS-041) | ProductPrice lookup with virtual/parent fallback. |
| `applyVatInPriceAdjustment` | rule loop (F-TAS-042) | VAT-in-price handling; returns `discountedSalesTax`. |
| `buildVatPriceCorrection` | rule loop (F-TAS-046) | Build the `VAT_PRICE_CORRECT` adjustment when entered ≠ calculated. |
| `applyPartyTaxExemptions` | rule loop (F-TAS-044) | Group-rollup gather, then delegate to `handlePartyTaxExempt`. |

`getTaxAdjustments` now reads as: gather query predicates → run the single rate query →
for each rate rule delegate to `buildTaxAdjustmentsForRateRule`. This separates
**rule evaluation from persistence lookups** without moving any query in or out of a loop.

Local names moved toward domain vocabulary (e.g. `lookupList` → `rateRuleList`; the per-rule
result is `ruleAdjustments`).

## Why behavior cannot have changed

* **Pure code motion.** Every extracted method contains the original statements verbatim in
  the same order. No arithmetic expression, scale, `RoundingMode`, `divide`/`multiply`
  operand order, or `PERCENT_SCALE`/`TAX_SCALE`/`TAX_FINAL_SCALE`/`TAX_ROUNDING` constant was
  altered. The bug-compatible `itemQuantity != BigDecimal.ZERO` reference comparison is kept
  exactly.
* **Same queries, same count, same order.** Entity queries (`TaxAuthorityRateProduct`,
  `TaxAuthorityGlAccount`, `ProductPrice`, `PartyRelationship`, `PartyTaxAuthInfo`,
  `TaxAuthorityAssoc`, `TaxAuthority`) keep identical `where`/`orderBy`/`cache`/`filterByDate`
  clauses and are executed at the same points inside the same loop iterations. `nowTimestamp`
  is still computed once per `getTaxAdjustments` call and threaded through, preserving the
  single-timestamp semantics for exemption filtering.
* **Identical output shape and ordering.** `buildTaxAdjustmentsForRateRule` returns a list
  built in the original add order (optional negative VAT adjustment, then the main adjustment,
  then optional VAT price correction); `addAll` preserves it. The `taxable == 0` case returns
  an empty list — equivalent to the original `continue`.
* **Exceptions unchanged.** All extracted persistence helpers still throw
  `GenericEntityException`, and their calls remain inside the same `try` in `getTaxAdjustments`
  whose `catch` returns `new LinkedList<>()`; on error the accumulated adjustments are still
  discarded exactly as before.
* **Logging preserved.** Every `Debug.logWarning/logInfo/logError/logVerbose` call is kept with
  the same message, level, and location on the control path.
* **Provenance-critical rules preserved:** party tax exemption zeroing, group rollup, and
  exemption inheritance (`handlePartyTaxExempt` + `applyPartyTaxExemptions`), and the threshold
  rules (`minItemPrice` / `minPurchase`) in `buildRateRuleLookupCondition`, are unchanged in
  logic — only relocated.
* **`AIRLIFT:` traceability comments** were moved together with the code they annotate
  (F-TAS-007, 031, 032, 034, 035, 036, 038, 039, 041, 042, 044, 045, 046, plus the unchanged
  049/051/052/054/055/056).

## Verification

* `./gradlew compileJava` (offline, `--rerun-tasks`) — **BUILD SUCCESSFUL**; the class was
  recompiled cleanly.
* No line exceeds the repository's 150-character checkstyle limit.
* Only the single seam file was modified; no test, service-def, entity-def, or config file was touched.
