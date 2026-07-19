package org.apache.ofbiz.accounting.tax.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a test method to the Airlift IR claim it verifies.
 * The verification spine maps failing tests back to violated claims through this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AirliftClaim {
    /** The Airlift claim ID, e.g. "TAX.EXEMPT.PARTY-EXEMPTION-ZEROES-TAX". */
    String value();
}
