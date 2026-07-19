package org.apache.ofbiz.accounting.tax.test;

/**
 * Harness self-test: proves the fixture DSL, service invocation, and result wrapper work
 * end-to-end inside the OFBiz test container before any generated tests rely on them.
 */
public class SmokeTaxTests extends AirliftTaxTestCase {

    public SmokeTaxTests(String name) {
        super(name);
    }

    public void testBasicSalesTaxCalculation() throws Exception {
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
        // 100.00 * 6.25% = 6.25, at 3 calc decimals -> 6.250
        assertDecimal("6.250", adj.amount());
        assertDecimal("6.25", adj.ratePercent());
        assertEquals(state, adj.jurisdictionGeo());
    }

    public void testExemptPartyPaysZero() throws Exception {
        TaxFixture fx = fixture();
        String country = fx.country();
        String state = fx.state(country);
        TaxFixture.Authority auth = fx.taxAuthority(state);
        fx.rate(auth).percent("10").create();
        String store = fx.store().create();
        String widget = fx.product().taxable(true).create();
        String buyer = fx.customer("Exempt");
        fx.registration(buyer, auth).taxId("EX-001").exempt(true).create();

        TaxCalcResult result = fx.calcTax()
                .store(store)
                .billTo(buyer)
                .shipTo(fx.address().country(country).state(state))
                .item(widget, "50.00", "2")
                .run();

        TaxCalcResult.Adjustment adj = result.single(0);
        assertDecimal("0", adj.amount());
        assertDecimal("10.000", adj.exemptAmount());
        assertEquals("EX-001", adj.customerTaxId());
    }
}
