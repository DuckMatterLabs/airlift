package org.apache.ofbiz.accounting.tax.test;

import java.math.BigDecimal;

import org.apache.ofbiz.service.testtools.OFBizTestCase;

/**
 * Base class for Airlift tax tests (backfill and blind-generated).
 * JUnit-3 style: subclasses declare {@code public MyTests(String name) { super(name); }}
 * and test methods named {@code public void testXxx() throws Exception}.
 */
public abstract class AirliftTaxTestCase extends OFBizTestCase {

    public AirliftTaxTestCase(String name) {
        super(name);
    }

    /** A fresh, isolated fixture namespace for one test method. */
    protected TaxFixture fixture() {
        return new TaxFixture(getDelegator(), getDispatcher());
    }

    /** Asserts numeric equality ignoring trailing zeros (10.5 equals 10.500). */
    protected static void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull("expected " + expected + " but was null", actual);
        assertTrue("expected " + expected + " but was " + actual.toPlainString(),
                new BigDecimal(expected).compareTo(actual) == 0);
    }

    /** Asserts an order line produced no tax adjustments at all. */
    protected static void assertNoAdjustments(TaxCalcResult result, int itemIndex) {
        assertTrue("expected no adjustments for item " + itemIndex + " but got "
                        + result.itemAdjustments(itemIndex),
                result.itemAdjustments(itemIndex).isEmpty());
    }
}
