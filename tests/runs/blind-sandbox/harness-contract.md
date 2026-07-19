# Airlift Tax Test-Harness Contract (target: ofbiz-tax)

This is the complete, authoritative API for writing tax-calculation integration tests.
It documents test **mechanics only** — what the system's behavior IS must come from the
Airlift IR (blind generation) or the source (backfill), never from this document.

## Test class shape

Tests are JUnit-3 style Java classes (no annotations-based JUnit 4/5):

* **Backfill tests**: package `org.apache.ofbiz.accounting.tax.test.backfill`, class
  `BackfillTaxTests`, file
  `applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/backfill/BackfillTaxTests.java`
  (path relative to the repository root).
* **Blind-generated tests**: package `org.apache.ofbiz.accounting.tax.test.blind`, class
  `BlindTaxClaimTests`, written to `generated/BlindTaxClaimTests.java` relative to YOUR
  working directory (an installer moves it into the repository).

Rules:
* Extend `org.apache.ofbiz.accounting.tax.test.AirliftTaxTestCase`.
* Declare exactly one constructor: `public <ClassName>(String name) { super(name); }`.
* Each test is `public void test<Name>() throws Exception`.
* Bind every test to its claim: place `@AirliftClaim("<CLAIM-ID>")` directly above the
  method (import `org.apache.ofbiz.accounting.tax.test.AirliftClaim`).
* Allowed imports: `org.apache.ofbiz.accounting.tax.test.*` (the harness),
  `java.math.BigDecimal`. NOTHING else beyond `java.*`. Never import `org.apache.ofbiz.*`
  outside the harness package; never touch the persistence or service layer directly.
* Each test method starts by creating its own isolated fixture: `TaxFixture fx = fixture();`
  All data created through one fixture is namespaced uniquely; tests must not share data.

## Fixture DSL (`TaxFixture fx = fixture()`)

Geography (jurisdiction identifiers are opaque strings):
* `String fx.country()` — a country-level jurisdiction geo.
* `String fx.state(countryGeoId)` — a state contained in the country.
* `String fx.county(stateGeoId)` — a county contained in the state.
* `void fx.containedIn(parentGeoId, childGeoId)` — declare extra region containment.

Parties:
* `String fx.organization(name)` — an organization party.
* `String fx.customer(lastName)` — an individual purchasing party.
* `void fx.groupRollup(groupPartyId, memberPartyId)` — membership of a party in a group.

Tax authorities and rate rules:
* `TaxFixture.Authority fx.taxAuthority(geoId)` — the authority governing that geo
  (handle exposes `.geoId()` and `.partyId()`).
* `TaxFixture.Authority fx.globalAuthority()` — the special catch-all authority.
* `fx.rate(authority)` — rate-rule builder; chain then `.create()`:
  `.percent("6.25")` `.minItemPrice("50.00")` `.minPurchase("100.00")`
  `.taxShipping(bool)` `.taxPromotions(bool)` `.store(storeId)` `.category(categoryId)`
  `.fromDaysFromNow(int)` `.thruDaysFromNow(int)` `.description(text)`.
  Options left uncalled remain UNSET (semantics of unset are an IR concern).
* `void fx.inheritExemption(childAuthority, parentAuthority)` — registrations with
  `parentAuthority` are consulted for `childAuthority`'s jurisdiction.
* `String fx.glAccountFor(authority, organizationPartyId)` — configures a ledger account
  for that authority/organization pair; returns its id.

Registrations (a party's standing with an authority):
* `fx.registration(partyId, authority)` builder; chain then `.create()`:
  `.taxId("EX-001")` `.exempt(bool)` `.fromDaysFromNow(int)` `.thruDaysFromNow(int)`.

Store and catalog:
* `fx.store()` builder; chain then `.create()` → storeId:
  `.payTo(partyId)` (defaults to a fresh organization) `.inDefaultStoreGroup()`
  `.vatDisplay(authority)`.
* `fx.product()` builder; chain then `.create()` → productId:
  `.taxable(bool)` `.taxableUnset()` `.variantOf(virtualProductId)` `.virtual()`.
* `String fx.category()`; `void fx.inCategory(productId, categoryId)`.
* `fx.price(productId, authority)` builder; chain then `.create()`:
  `.price("35.83")` `.priceWithTax("43.00")` `.taxInPrice(bool)`.

Destination:
* `fx.address()` builder (no `.create()` needed):
  `.country(geoId)` `.state(geoId)` `.county(geoId)` `.postalCodeGeo(geoId)`.
* `String fx.facility(address)` — a physical shop configured at that address.

Invocation:
* `fx.calcTax()` builder:
  `.store(storeId)` or `.payTo(partyId)`; `.billTo(partyId)`; `.shipTo(address)` or
  `.atFacility(facilityId)`;
  `.item(productId, unitPrice, quantity)` (line amount = price x quantity) or
  `.itemWithAmount(productId, unitPrice, quantity, lineAmount)`;
  `.lineShipping(amount)` (applies to the most recent line);
  `.orderShipping(amount)`; `.orderPromotions(amount)`;
  then `.run()` → `TaxCalcResult`, or `.runExpectingError()` → error message String.
* `fx.calcTaxForDisplay(storeId, productId, basePrice, quantity, shippingPrice)` →
  `DisplayResult` with `.taxTotal()`, `.taxPercentage()`, `.priceWithTax()`
  (quantity/shippingPrice may be null).

Results (`TaxCalcResult`):
* `.itemAdjustments(i)` — list of `Adjustment` for line i (0-based); `.single(i)` asserts
  exactly one. `.orderAdjustments()`, `.singleOrderAdjustment()`. `.itemCount()`.
* `Adjustment` accessors: `.type()` (one of `"SALES_TAX"`, `"VAT_TAX"`,
  `"VAT_PRICE_CORRECT"`), `.amount()`, `.exemptAmount()`, `.includedTax()`,
  `.ratePercent()`, `.jurisdictionGeo()`, `.authorityParty()`, `.primaryGeo()`,
  `.customerTaxId()`, `.glAccountOverride()`, `.description()` — all BigDecimal or String.

Assertion helpers (inherited):
* `assertDecimal("6.250", adj.amount())` — numeric equality ignoring trailing zeros.
  Use this for ALL monetary/percentage assertions.
* `assertNoAdjustments(result, itemIndex)`.
* Plain JUnit-3 asserts are available: `assertEquals`, `assertTrue`, `assertNull`, `fail`.

## Worked example (compiles as-is)

```java
package org.apache.ofbiz.accounting.tax.test.blind;

import org.apache.ofbiz.accounting.tax.test.AirliftClaim;
import org.apache.ofbiz.accounting.tax.test.AirliftTaxTestCase;
import org.apache.ofbiz.accounting.tax.test.TaxCalcResult;
import org.apache.ofbiz.accounting.tax.test.TaxFixture;

public class BlindTaxClaimTests extends AirliftTaxTestCase {

    public BlindTaxClaimTests(String name) {
        super(name);
    }

    @AirliftClaim("TAX.RATE.EXAMPLE-CLAIM-ID")
    public void testStateRateAppliesToShipment() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        assertEquals("SALES_TAX", adj.type());
        // 100.00 * 6.25% = 6.2500 -> 6.250 at 3 decimals
        assertDecimal("6.250", adj.amount());
    }
}
```

## Running (backfill stage only; blind generation cannot run tests)

From the repository root:
* Backfill suite: `./gradlew "ofbiz --test component=accounting --test suitename=airliftbackfill" --console=plain`
* Compile check only: `./gradlew compileTestJava -x checkstyleMain -x checkstyleTest --console=plain`
* JUnit XML report: `runtime/logs/test-results/airliftbackfill.xml`; the gradle command
  exits non-zero when any test fails.

Environment facts (mechanics, not behavior): the database is pre-loaded with framework
reference data only — no tax authorities, no rate rules, no registrations exist unless a
fixture creates them. All fixture data is rolled back after the suite. Monetary amounts and
percentages are decimal strings.
