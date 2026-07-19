package org.apache.ofbiz.accounting.tax.test.blind;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ofbiz.accounting.tax.test.AirliftClaim;
import org.apache.ofbiz.accounting.tax.test.AirliftTaxTestCase;
import org.apache.ofbiz.accounting.tax.test.TaxCalcResult;
import org.apache.ofbiz.accounting.tax.test.TaxFixture;

public class BlindTaxClaimTests extends AirliftTaxTestCase {

    public BlindTaxClaimTests(String name) {
        super(name);
    }

    // ---------------------------------------------------------------------
    // TAX.CALC.TAX-AMOUNT
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.CALC.TAX-AMOUNT")
    public void testTaxAmountIsBaseTimesRate() throws Exception {
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
        // 100.00 * 6.25% = 6.2500 -> 6.250 at calculation-precision (3dp, half-up)
        assertDecimal("6.250", adj.amount());
        assertDecimal("6.25", adj.ratePercent());
    }

    // ---------------------------------------------------------------------
    // TAX.CALC.TAXABLE-BASE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.CALC.TAXABLE-BASE")
    public void testTaxableProductIsTaxed() throws Exception {
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
        // 100.00 * 6.25% = 6.250
        assertDecimal("6.250", adj.amount());
    }

    @AirliftClaim("TAX.CALC.TAXABLE-BASE")
    public void testNonTaxableProductYieldsNoLine() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(false).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // Non-taxable product: taxable base is zero, so no tax adjustment at all.
        assertNoAdjustments(result, 0);
    }

    // ---------------------------------------------------------------------
    // TAX.CONFIG.CALC-PRECISION (config claim: shapes the 3dp half-up amounts)
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.CONFIG.CALC-PRECISION")
    public void testCalcPrecisionHalfUpAtThirdDecimal() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("7").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "10.05", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // 10.05 * 7% = 0.7035 -> half-up at 3dp -> 0.704
        assertDecimal("0.704", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.CONFIG.FINAL-PRECISION (config claim: shapes the 2dp half-up display totals)
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.CONFIG.FINAL-PRECISION")
    public void testFinalPrecisionHalfUpAtSecondDecimal() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        TaxFixture.Authority auth = fx.taxAuthority(country);
        fx.rate(auth).percent("6").create();
        String store = fx.store().vatDisplay(auth).create();
        String widget = fx.product().taxable(true).create();

        TaxFixture.DisplayResult result = fx.calcTaxForDisplay(store, widget, "104.25", "1", null);

        // base 104.25; tax 104.25*6% = 6.255 (3dp) -> final 2dp half-up -> 6.26
        // price 104.25 + 6.255 = 110.505 -> final 2dp half-up -> 110.51
        assertDecimal("6.26", result.taxTotal());
        assertDecimal("6", result.taxPercentage());
        assertDecimal("110.51", result.priceWithTax());
    }

    // ---------------------------------------------------------------------
    // TAX.EXEMPT.EFFECTIVE-INFO
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.EXEMPT.EFFECTIVE-INFO")
    public void testEffectiveRegistrationIdRecordedNonExemptStillTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String buyer = fx.customer("Doe");
        fx.registration(buyer, auth).taxId("EX-001").exempt(false).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .billTo(buyer)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // Non-exempt registration: id recorded, tax still charged. 100.00*6.25% = 6.250
        assertDecimal("6.250", adj.amount());
        assertEquals("EX-001", adj.customerTaxId());
    }

    // ---------------------------------------------------------------------
    // TAX.EXEMPT.ZEROING
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.EXEMPT.ZEROING")
    public void testExemptPartyPaysZero() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String buyer = fx.customer("Doe");
        fx.registration(buyer, auth).taxId("EX-001").exempt(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .billTo(buyer)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // Exempt: charged tax zeroed, would-be tax 100.00*6.25% = 6.250 recorded as exempt.
        assertDecimal("0", adj.amount());
        assertDecimal("6.250", adj.exemptAmount());
        assertEquals("EX-001", adj.customerTaxId());
    }

    // ---------------------------------------------------------------------
    // TAX.EXEMPT.GROUP-ROLLUP
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.EXEMPT.GROUP-ROLLUP")
    public void testMemberInheritsParentGroupExemption() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String group = fx.organization("Acme");
        String member = fx.customer("Doe");
        fx.groupRollup(group, member);
        fx.registration(group, auth).taxId("EX-GRP").exempt(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .billTo(member)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // Member has no own exemption but rolls up to an exempt group: tax zeroed,
        // would-be tax 100.00*6.25% = 6.250 recorded as exempt.
        assertDecimal("0", adj.amount());
        assertDecimal("6.250", adj.exemptAmount());
    }

    // ---------------------------------------------------------------------
    // TAX.EXEMPT.JURISDICTION-INHERITANCE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.EXEMPT.JURISDICTION-INHERITANCE")
    public void testCountyTaxExemptViaStateExemption() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        String county = fx.county(state);
        TaxFixture.Authority stateAuth = fx.taxAuthority(state);
        TaxFixture.Authority countyAuth = fx.taxAuthority(county);
        fx.rate(countyAuth).percent("2").create();
        fx.inheritExemption(countyAuth, stateAuth);
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String buyer = fx.customer("Doe");
        fx.registration(buyer, stateAuth).taxId("EX-ST").exempt(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .billTo(buyer)
                .shipTo(fx.address().country(country).state(state).county(county))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // Exemption held in the parent (state) jurisdiction exempts the child (county) tax.
        // County tax 100.00*2% = 2.000 becomes the exempt-amount; charged tax is zero.
        assertDecimal("2", adj.ratePercent());
        assertDecimal("0", adj.amount());
        assertDecimal("2.000", adj.exemptAmount());
    }

    // ---------------------------------------------------------------------
    // TAX.JURIS.ADDRESS-REQUIRED
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.JURIS.ADDRESS-REQUIRED")
    public void testEmptyDestinationErrors() throws Exception {
        TaxFixture fx = fixture();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        // Destination names no country, state or postal-code-area: must fail rather than
        // return tax lines.
        String error = fx.calcTax()
                .store(store)
                .shipTo(fx.address())
                .item(widget, "100.00", "1")
                .runExpectingError();

        assertTrue("expected a jurisdiction error", error != null && error.length() > 0);
    }

    // ---------------------------------------------------------------------
    // TAX.JURIS.ADDRESS-TO-JURISDICTION
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.JURIS.ADDRESS-TO-JURISDICTION")
    public void testStateAddressYieldsStateJurisdiction() throws Exception {
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
        // Jurisdiction on the tax line derives from the state named in the address.
        assertEquals(state, adj.jurisdictionGeo());
        assertDecimal("6.250", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.JURIS.CONTAINMENT
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.JURIS.CONTAINMENT")
    public void testEnclosingCountryAuthorityAppliesToStateNamedAddress() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country); // state is contained in the country
        TaxFixture.Authority auth = fx.taxAuthority(country);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().state(state))
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // Address names only a state; the enclosing country authority applies via containment.
        assertEquals(country, adj.jurisdictionGeo());
        assertDecimal("6.250", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.JURIS.FACILITY-FALLBACK
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.JURIS.FACILITY-FALLBACK")
    public void testFacilityAddressTaxedWhenNoShippingDestination() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String facility = fx.facility(fx.address().country(country).state(state));

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .atFacility(facility)
                .item(widget, "100.00", "1")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        // No destination address: the facility's state supplies the jurisdiction.
        assertEquals(state, adj.jurisdictionGeo());
        assertDecimal("6.250", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.ORDER.FREIGHT-SPLIT
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.ORDER.FREIGHT-SPLIT")
    public void testOrderFreightTaxSplitByProductValue() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxShipping(true).create();
        String store = fx.store().create();
        String big = fx.product().taxable(true).create();
        String small = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(big, "30.00", "1")
                .item(small, "10.00", "1")
                .orderShipping("10.00")
                .run();

        // value-weights: 30/40 = 0.75 and 10/40 = 0.25.
        // freight tax per item = 10.00 * (6.25 * weight)% :
        //   10.00 * 4.6875% = 0.46875 -> 0.469
        //   10.00 * 1.5625% = 0.15625 -> 0.156
        // Order-adjustment ordering is not guaranteed; assert as a set.
        List<TaxCalcResult.Adjustment> orderAdjs = result.orderAdjustments();
        assertEquals(2, orderAdjs.size());
        boolean found469 = false;
        boolean found156 = false;
        for (TaxCalcResult.Adjustment adj : orderAdjs) {
            if (adj.amount().compareTo(new BigDecimal("0.469")) == 0) {
                found469 = true;
            }
            if (adj.amount().compareTo(new BigDecimal("0.156")) == 0) {
                found156 = true;
            }
        }
        assertTrue("expected freight tax 0.469", found469);
        assertTrue("expected freight tax 0.156", found156);
    }

    // ---------------------------------------------------------------------
    // TAX.ORDER.PER-ITEM
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.ORDER.PER-ITEM")
    public void testTwoLinesTaxedSeparately() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").create();
        String store = fx.store().create();
        String a = fx.product().taxable(true).create();
        String b = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(a, "100.00", "1")
                .item(b, "40.00", "1")
                .run();

        assertEquals(2, result.itemCount());
        // Each line taxed on its own base: 100.00*6.25% = 6.250; 40.00*6.25% = 2.500
        assertDecimal("6.250", result.single(0).amount());
        assertDecimal("2.500", result.single(1).amount());
    }

    // ---------------------------------------------------------------------
    // TAX.ORDER.PROMO-TAX
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.ORDER.PROMO-TAX")
    public void testOrderPromotionsProduceOrderLevelTaxLine() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxPromotions(true).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .orderPromotions("100.00")
                .run();

        TaxCalcResult.Adjustment adj = result.singleOrderAdjustment();
        // A single order-level tax line for promotions: 100.00*6.25% = 6.250
        assertDecimal("6.250", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.AUTHORITY-MATCH
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.AUTHORITY-MATCH")
    public void testResolvedAuthorityRuleDrivesTheTaxLine() throws Exception {
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
        // rate-percentage and jurisdiction-geo come from the resolved authority's rule.
        assertDecimal("6.25", adj.ratePercent());
        assertEquals(state, adj.jurisdictionGeo());
        assertDecimal("6.250", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.CATEGORY-MATCH
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.CATEGORY-MATCH")
    public void testCategoryRuleTaxesProductInCategory() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        String cat = fx.category();
        fx.rate(auth).percent("6.25").category(cat).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        fx.inCategory(widget, cat);

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // Product in the rule's category is taxed: 100.00*6.25% = 6.250
        assertDecimal("6.250", result.single(0).amount());
    }

    @AirliftClaim("TAX.RATE.CATEGORY-MATCH")
    public void testCategoryRuleSkipsProductNotInCategory() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        String cat = fx.category();
        fx.rate(auth).percent("6.25").category(cat).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // Product in no category: the category-restricted rule does not apply.
        assertNoAdjustments(result, 0);
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.PROMO-TAXABLE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.PROMO-TAXABLE")
    public void testPromotionsTaxedWhenRuleAllows() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxPromotions(true).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .orderPromotions("100.00")
                .run();

        // Rule permits taxing promotions: 100.00*6.25% = 6.250
        assertDecimal("6.250", result.singleOrderAdjustment().amount());
    }

    @AirliftClaim("TAX.RATE.PROMO-TAXABLE")
    public void testPromotionsNotTaxedWhenRuleForbids() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxPromotions(false).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .orderPromotions("100.00")
                .run();

        // Rule forbids taxing promotions: no order-level tax adjustment.
        assertEquals(0, result.orderAdjustments().size());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.SHIPPING-TAXABLE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.SHIPPING-TAXABLE")
    public void testFreightTaxedWhenRuleAllows() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxShipping(true).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .orderShipping("10.00")
                .run();

        // Single product carries the whole order value (weight 1): 10.00*6.25% = 0.625
        assertDecimal("0.625", result.singleOrderAdjustment().amount());
    }

    @AirliftClaim("TAX.RATE.SHIPPING-TAXABLE")
    public void testFreightNotTaxedWhenRuleForbids() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").taxShipping(false).create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .orderShipping("10.00")
                .run();

        // Rule forbids taxing shipping: no order-level tax adjustment.
        assertEquals(0, result.orderAdjustments().size());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.STORE-SCOPE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.STORE-SCOPE")
    public void testStoreScopedRuleAppliesToItsStore() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        String store = fx.store().create();
        fx.rate(auth).percent("6.25").store(store).create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // Rule scoped to this store applies: 100.00*6.25% = 6.250
        assertDecimal("6.250", result.single(0).amount());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.THRESHOLD-MIN-ITEM-PRICE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-ITEM-PRICE")
    public void testMinItemPriceAtThresholdIsTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minItemPrice("50.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "50.00", "1")
                .run();

        // unit-price 50.00 == minItemPrice 50.00 (inclusive): 50.00*6.25% = 3.125
        assertDecimal("3.125", result.single(0).amount());
    }

    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-ITEM-PRICE")
    public void testMinItemPriceBelowThresholdIsNotTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minItemPrice("50.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "49.99", "1")
                .run();

        // unit-price 49.99 < minItemPrice 50.00: rule does not apply.
        assertNoAdjustments(result, 0);
    }

    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-ITEM-PRICE")
    public void testMinItemPriceAboveThresholdIsTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minItemPrice("50.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // unit-price 100.00 > minItemPrice 50.00: 100.00*6.25% = 6.250
        assertDecimal("6.250", result.single(0).amount());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.THRESHOLD-MIN-PURCHASE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-PURCHASE")
    public void testMinPurchaseAtThresholdIsTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minPurchase("100.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "100.00", "1")
                .run();

        // line-amount 100.00 == minPurchase 100.00 (inclusive): 100.00*6.25% = 6.250
        assertDecimal("6.250", result.single(0).amount());
    }

    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-PURCHASE")
    public void testMinPurchaseBelowThresholdIsNotTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minPurchase("100.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "99.99", "1")
                .run();

        // line-amount 99.99 < minPurchase 100.00: rule does not apply.
        assertNoAdjustments(result, 0);
    }

    @AirliftClaim("TAX.RATE.THRESHOLD-MIN-PURCHASE")
    public void testMinPurchaseAboveThresholdIsTaxed() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("6.25").minPurchase("100.00").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "250.00", "1")
                .run();

        // line-amount 250.00 > minPurchase 100.00: 250.00*6.25% = 15.625
        assertDecimal("15.625", result.single(0).amount());
    }

    // ---------------------------------------------------------------------
    // TAX.RATE.VARIANT-CATEGORY
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.RATE.VARIANT-CATEGORY")
    public void testVariantTaxedViaParentCategory() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        String cat = fx.category();
        fx.rate(auth).percent("6.25").category(cat).create();
        String store = fx.store().create();
        String virtual = fx.product().virtual().create();
        fx.inCategory(virtual, cat);
        String variant = fx.product().variantOf(virtual).taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(variant, "100.00", "1")
                .run();

        // Variant belongs to no category itself, but its virtual parent is in cat:
        // the category rule applies. 100.00*6.25% = 6.250
        assertDecimal("6.250", result.single(0).amount());
    }

    // ---------------------------------------------------------------------
    // TAX.VAT.PRICE-INCLUSIVE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.VAT.PRICE-INCLUSIVE")
    public void testTaxInclusivePriceReportsIncludedTaxNotAdded() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("20").create();
        String store = fx.store().inDefaultStoreGroup().create();
        String widget = fx.product().taxable(true).create();
        fx.price(widget, auth).price("35.83").taxInPrice(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "43.00", "3")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        assertEquals("VAT_TAX", adj.type());
        // per-unit embedded tax = 43.00 - 43.00/1.20 = 43.00 - 35.83 = 7.17; * 3 = 21.51
        // charged tax is zero because it is already inside the price.
        assertDecimal("21.51", adj.includedTax());
        assertDecimal("0", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.VAT.VARIANT-PRICE
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.VAT.VARIANT-PRICE")
    public void testVariantUsesParentVatPrice() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("20").create();
        String store = fx.store().inDefaultStoreGroup().create();
        String virtual = fx.product().virtual().create();
        fx.price(virtual, auth).price("35.83").taxInPrice(true).create();
        String variant = fx.product().variantOf(virtual).taxable(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .item(variant, "43.00", "3")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        assertEquals("VAT_TAX", adj.type());
        // Variant has no own price record: falls back to the virtual parent's tax-inclusive
        // price. per-unit tax 7.17 * 3 = 21.51; charged tax zero.
        assertDecimal("21.51", adj.includedTax());
        assertDecimal("0", adj.amount());
    }

    // ---------------------------------------------------------------------
    // TAX.VAT.DISCOUNT-CORRECTION
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.VAT.DISCOUNT-CORRECTION")
    public void testDiscountedTaxInclusiveItemEmitsNegativeIncludedTax() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("20").create();
        String store = fx.store().inDefaultStoreGroup().create();
        String widget = fx.product().taxable(true).create();
        fx.price(widget, auth).price("35.83").taxInPrice(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .itemWithAmount(widget, "43.00", "3", "107.50")
                .run();

        // full-price embedded tax = (43.00 - 35.83) * 3 = 21.51
        // net tax on discounted line 107.50 = (35.83 - 29.86) * 3 = 17.91
        // correction = 17.91 - 21.51 = -3.60 (emitted as a separate negative line)
        List<TaxCalcResult.Adjustment> adjs = result.itemAdjustments(0);
        assertEquals(2, adjs.size());
        boolean foundFull = false;
        boolean foundCorrection = false;
        for (TaxCalcResult.Adjustment adj : adjs) {
            if (adj.includedTax().compareTo(new BigDecimal("21.51")) == 0) {
                foundFull = true;
            }
            if (adj.includedTax().compareTo(new BigDecimal("-3.60")) == 0) {
                foundCorrection = true;
            }
        }
        assertTrue("expected full-price included-tax 21.51", foundFull);
        assertTrue("expected negative correction included-tax -3.60", foundCorrection);
    }

    // ---------------------------------------------------------------------
    // TAX.VAT.PRICE-CORRECTION
    // ---------------------------------------------------------------------
    @AirliftClaim("TAX.VAT.PRICE-CORRECTION")
    public void testPriceWithTaxRoundingReconciledByCorrectionLine() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("20").create();
        String store = fx.store().inDefaultStoreGroup().create();
        String widget = fx.product().taxable(true).create();
        fx.price(widget, auth).price("35.83").priceWithTax("43.00").taxInPrice(false).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .shipTo(fx.address().country(country).state(state))
                .itemWithAmount(widget, "35.83", "3", "107.49")
                .run();

        // base subtotal = 35.83*3 = 107.49; computed tax = 107.49*20% = 21.498
        // computed gross = 107.49 + 21.498 = 128.988; entered gross = 43.00*3 = 129.00
        // correction = 129.00 - 128.988 = 0.012
        List<TaxCalcResult.Adjustment> adjs = result.itemAdjustments(0);
        assertEquals(2, adjs.size());
        boolean foundTax = false;
        boolean foundCorrection = false;
        for (TaxCalcResult.Adjustment adj : adjs) {
            if ("SALES_TAX".equals(adj.type())) {
                foundTax = true;
                assertDecimal("21.498", adj.amount());
            }
            if ("VAT_PRICE_CORRECT".equals(adj.type())) {
                foundCorrection = true;
                assertDecimal("0.012", adj.amount());
            }
        }
        assertTrue("expected the ordinary sales-tax line", foundTax);
        assertTrue("expected the price-correction line", foundCorrection);
    }
}
