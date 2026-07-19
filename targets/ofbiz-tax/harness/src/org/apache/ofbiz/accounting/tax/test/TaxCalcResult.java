package org.apache.ofbiz.accounting.tax.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.entity.GenericValue;

/**
 * Simplified, architecture-free view of a tax-calculation response.
 * Wraps the raw service output so tests never touch framework types.
 */
public final class TaxCalcResult {

    /** One tax adjustment produced by the calculation. */
    public static final class Adjustment {
        private final GenericValue raw;

        Adjustment(GenericValue raw) {
            this.raw = raw;
        }

        /** Adjustment kind: "SALES_TAX", "VAT_TAX" or "VAT_PRICE_CORRECT". */
        public String type() {
            return raw.getString("orderAdjustmentTypeId");
        }

        /** Tax amount charged. Zero when the party is exempt. */
        public BigDecimal amount() {
            return raw.getBigDecimal("amount");
        }

        /** Tax that WOULD have been charged but was forgiven by an exemption. */
        public BigDecimal exemptAmount() {
            return raw.getBigDecimal("exemptAmount");
        }

        /** Tax already contained in a tax-inclusive price (VAT-in-price mode). */
        public BigDecimal includedTax() {
            return raw.getBigDecimal("amountAlreadyIncluded");
        }

        /** The applied rate, in percent. */
        public BigDecimal ratePercent() {
            return raw.getBigDecimal("sourcePercentage");
        }

        /** Geo id of the taxing jurisdiction this adjustment belongs to. */
        public String jurisdictionGeo() {
            return raw.getString("taxAuthGeoId");
        }

        /** Party id of the tax authority organization. */
        public String authorityParty() {
            return raw.getString("taxAuthPartyId");
        }

        /** Primary geo recorded on the adjustment (main jurisdiction). */
        public String primaryGeo() {
            return raw.getString("primaryGeoId");
        }

        /** The purchasing party's registered tax id in the jurisdiction, when on file. */
        public String customerTaxId() {
            return raw.getString("customerReferenceId");
        }

        /** Ledger account override attached for the authority, if configured. */
        public String glAccountOverride() {
            return raw.getString("overrideGlAccountId");
        }

        /** Human description carried from the matched rate rule. */
        public String description() {
            return raw.getString("comments");
        }

        @Override
        public String toString() {
            return "Adjustment(" + type() + " amount=" + amount() + " exempt=" + exemptAmount()
                    + " rate=" + ratePercent() + " geo=" + jurisdictionGeo() + ")";
        }
    }

    private final List<Adjustment> orderAdjustments = new ArrayList<>();
    private final List<List<Adjustment>> itemAdjustments = new ArrayList<>();

    @SuppressWarnings("unchecked")
    TaxCalcResult(Map<String, Object> serviceResult) {
        for (GenericValue gv : (List<GenericValue>) serviceResult.get("orderAdjustments")) {
            orderAdjustments.add(new Adjustment(gv));
        }
        for (List<GenericValue> line : (List<List<GenericValue>>) serviceResult.get("itemAdjustments")) {
            List<Adjustment> conv = new ArrayList<>();
            for (GenericValue gv : line) {
                conv.add(new Adjustment(gv));
            }
            itemAdjustments.add(conv);
        }
    }

    /** Order-level adjustments (freight tax, promotion tax). */
    public List<Adjustment> orderAdjustments() {
        return orderAdjustments;
    }

    /** Number of order lines in the response (parallel to the request lines). */
    public int itemCount() {
        return itemAdjustments.size();
    }

    /** All adjustments for the order line at {@code itemIndex} (0-based). */
    public List<Adjustment> itemAdjustments(int itemIndex) {
        return itemAdjustments.get(itemIndex);
    }

    /** The single adjustment expected for a line; fails the test if there are 0 or many. */
    public Adjustment single(int itemIndex) {
        List<Adjustment> line = itemAdjustments.get(itemIndex);
        if (line.size() != 1) {
            throw new IllegalStateException("expected exactly 1 adjustment for item " + itemIndex
                    + " but got " + line.size() + ": " + line);
        }
        return line.get(0);
    }

    /** The single order-level adjustment; fails the test if there are 0 or many. */
    public Adjustment singleOrderAdjustment() {
        if (orderAdjustments.size() != 1) {
            throw new IllegalStateException("expected exactly 1 order adjustment but got "
                    + orderAdjustments.size() + ": " + orderAdjustments);
        }
        return orderAdjustments.get(0);
    }
}
