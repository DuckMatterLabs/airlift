package org.apache.ofbiz.accounting.tax.test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Airlift tax test fixture DSL.
 *
 * Creates isolated, uniquely-identified tax configuration (jurisdictions, authorities,
 * rate rules, stores, products, parties, exemptions) and invokes the tax calculation,
 * hiding every framework mechanism from the test author. All records are created through
 * the test delegator and are rolled back after the suite.
 */
public final class TaxFixture {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final String NA = "_NA_";

    private final Delegator delegator;
    private final LocalDispatcher dispatcher;
    private final String ns;
    private final AtomicInteger local = new AtomicInteger(0);
    private final Timestamp past = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -1);

    public TaxFixture(Delegator delegator, LocalDispatcher dispatcher) {
        this.delegator = delegator;
        this.dispatcher = dispatcher;
        this.ns = "AL" + SEQ.incrementAndGet();
    }

    private String id(String tag) {
        return ns + tag + local.incrementAndGet();
    }

    private GenericValue create(String entity, Object... fields) {
        try {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < fields.length; i += 2) {
                map.put((String) fields[i], fields[i + 1]);
            }
            return delegator.create(delegator.makeValue(entity, map));
        } catch (Exception e) {
            throw new IllegalStateException("fixture: cannot create " + entity, e);
        }
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    // ------------------------------------------------------------------ geography

    /** A country jurisdiction geo. Returns its geo id. */
    public String country() {
        String geoId = id("C");
        create("Geo", "geoId", geoId, "geoTypeId", "COUNTRY", "geoName", geoId, "abbreviation", geoId);
        return geoId;
    }

    /** A state/province geo contained in {@code countryGeoId}. Returns its geo id. */
    public String state(String countryGeoId) {
        String geoId = id("S");
        create("Geo", "geoId", geoId, "geoTypeId", "STATE", "geoName", geoId, "abbreviation", geoId);
        containedIn(countryGeoId, geoId);
        return geoId;
    }

    /** A county geo contained in {@code stateGeoId}. Returns its geo id. */
    public String county(String stateGeoId) {
        String geoId = id("Y");
        create("Geo", "geoId", geoId, "geoTypeId", "COUNTY", "geoName", geoId, "abbreviation", geoId);
        containedIn(stateGeoId, geoId);
        return geoId;
    }

    /** Declares that {@code childGeoId} lies inside {@code parentGeoId} (region containment). */
    public void containedIn(String parentGeoId, String childGeoId) {
        create("GeoAssoc", "geoId", parentGeoId, "geoIdTo", childGeoId, "geoAssocTypeId", "REGIONS");
    }

    // ------------------------------------------------------------------ parties

    /** An organization party (e.g. a tax authority body or a store owner). Returns party id. */
    public String organization(String name) {
        String partyId = id("O");
        create("Party", "partyId", partyId, "partyTypeId", "PARTY_GROUP", "statusId", "PARTY_ENABLED");
        create("PartyGroup", "partyId", partyId, "groupName", name);
        create("PartyRole", "partyId", partyId, "roleTypeId", NA);
        return partyId;
    }

    /** An individual purchasing party. Returns party id. */
    public String customer(String lastName) {
        String partyId = id("P");
        create("Party", "partyId", partyId, "partyTypeId", "PERSON", "statusId", "PARTY_ENABLED");
        create("Person", "partyId", partyId, "lastName", lastName);
        create("PartyRole", "partyId", partyId, "roleTypeId", NA);
        return partyId;
    }

    /** Makes {@code memberPartyId} a member of the party group {@code groupPartyId}. */
    public void groupRollup(String groupPartyId, String memberPartyId) {
        create("PartyRelationship",
                "partyIdFrom", groupPartyId, "partyIdTo", memberPartyId,
                "roleTypeIdFrom", NA, "roleTypeIdTo", NA,
                "fromDate", past, "partyRelationshipTypeId", "GROUP_ROLLUP");
    }

    // ------------------------------------------------------------------ authorities & rates

    /** Handle for a tax authority: the pair (jurisdiction geo, authority organization party). */
    public static final class Authority {
        private final String geoId;
        private final String partyId;

        Authority(String geoId, String partyId) {
            this.geoId = geoId;
            this.partyId = partyId;
        }

        public String geoId() {
            return geoId;
        }

        public String partyId() {
            return partyId;
        }
    }

    /** A tax authority governing {@code geoId} (creates its organization party too). */
    public Authority taxAuthority(String geoId) {
        String partyId = organization("TaxAuthority-" + geoId);
        create("TaxAuthority", "taxAuthGeoId", geoId, "taxAuthPartyId", partyId);
        return new Authority(geoId, partyId);
    }

    /** The global catch-all authority (applies regardless of destination). Idempotent. */
    public Authority globalAuthority() {
        try {
            GenericValue existing = EntityQuery.use(delegator).from("TaxAuthority")
                    .where("taxAuthGeoId", NA, "taxAuthPartyId", NA).queryOne();
            if (existing == null) {
                create("TaxAuthority", "taxAuthGeoId", NA, "taxAuthPartyId", NA);
            }
        } catch (Exception e) {
            throw new IllegalStateException("fixture: global authority", e);
        }
        return new Authority(NA, NA);
    }

    /**
     * Exemptions registered with {@code parent} also count in {@code child}'s jurisdiction
     * (exemption inheritance).
     */
    public void inheritExemption(Authority child, Authority parent) {
        create("TaxAuthorityAssoc",
                "taxAuthGeoId", parent.geoId(), "taxAuthPartyId", parent.partyId(),
                "toTaxAuthGeoId", child.geoId(), "toTaxAuthPartyId", child.partyId(),
                "fromDate", past, "taxAuthorityAssocTypeId", "EXEMPT_INHER");
    }

    /** Configures the ledger account used for {@code authority} taxes owed by {@code organizationPartyId}. */
    public String glAccountFor(Authority authority, String organizationPartyId) {
        String glAccountId = id("G");
        create("GlAccount", "glAccountId", glAccountId, "accountName", "Airlift tax " + glAccountId);
        create("TaxAuthorityGlAccount",
                "taxAuthGeoId", authority.geoId(), "taxAuthPartyId", authority.partyId(),
                "organizationPartyId", organizationPartyId, "glAccountId", glAccountId);
        return glAccountId;
    }

    /** Builder for a tax rate rule of an authority. */
    public final class RateRule {
        private final Authority authority;
        private final Map<String, Object> fields = new HashMap<>();

        RateRule(Authority authority) {
            this.authority = authority;
            fields.put("taxAuthorityRateTypeId", "SALES_TAX");
            fields.put("fromDate", past);
            fields.put("description", "Airlift rate rule");
        }

        /** Tax percentage, e.g. "6.25". Required. */
        public RateRule percent(String percentage) {
            fields.put("taxPercentage", dec(percentage));
            return this;
        }

        /** Rule applies only to items with unit price at or above this. */
        public RateRule minItemPrice(String amount) {
            fields.put("minItemPrice", dec(amount));
            return this;
        }

        /** Rule applies only to lines whose line amount is at or above this. */
        public RateRule minPurchase(String amount) {
            fields.put("minPurchase", dec(amount));
            return this;
        }

        /** Whether this jurisdiction taxes shipping charges (default: unset = taxes them). */
        public RateRule taxShipping(boolean flag) {
            fields.put("taxShipping", flag ? "Y" : "N");
            return this;
        }

        /** Whether this jurisdiction taxes promotion amounts (default: unset = taxes them). */
        public RateRule taxPromotions(boolean flag) {
            fields.put("taxPromotions", flag ? "Y" : "N");
            return this;
        }

        /** Restrict the rule to one store. */
        public RateRule store(String productStoreId) {
            fields.put("productStoreId", productStoreId);
            return this;
        }

        /** Restrict the rule to products in a category. */
        public RateRule category(String productCategoryId) {
            fields.put("productCategoryId", productCategoryId);
            return this;
        }

        /** Rule validity start (days relative to now, negative = past). Default: -1. */
        public RateRule fromDaysFromNow(int days) {
            fields.put("fromDate", UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), days));
            return this;
        }

        /** Rule validity end (days relative to now, negative = past = already expired). */
        public RateRule thruDaysFromNow(int days) {
            fields.put("thruDate", UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), days));
            return this;
        }

        public RateRule description(String text) {
            fields.put("description", text);
            return this;
        }

        /** Creates the rule; returns its id. */
        public String create() {
            String seqId = delegator.getNextSeqId("TaxAuthorityRateProduct");
            fields.put("taxAuthorityRateSeqId", seqId);
            fields.put("taxAuthGeoId", authority.geoId());
            fields.put("taxAuthPartyId", authority.partyId());
            try {
                delegator.create(delegator.makeValue("TaxAuthorityRateProduct", fields));
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot create rate rule", e);
            }
            return seqId;
        }
    }

    /** Starts a rate-rule definition for {@code authority}. */
    public RateRule rate(Authority authority) {
        return new RateRule(authority);
    }

    // ------------------------------------------------------------------ exemptions

    /** Builder for a party's registration with a tax authority (tax id and/or exemption). */
    public final class Registration {
        private final Map<String, Object> fields = new HashMap<>();

        Registration(String partyId, Authority authority) {
            fields.put("partyId", partyId);
            fields.put("taxAuthGeoId", authority.geoId());
            fields.put("taxAuthPartyId", authority.partyId());
            fields.put("fromDate", past);
        }

        /** The party's tax id in this jurisdiction (appears on adjustments). */
        public Registration taxId(String taxId) {
            fields.put("partyTaxId", taxId);
            return this;
        }

        /** Whether the party is tax-exempt in this jurisdiction. */
        public Registration exempt(boolean flag) {
            fields.put("isExempt", flag ? "Y" : "N");
            return this;
        }

        /** Registration validity start (days relative to now). Default: -1. */
        public Registration fromDaysFromNow(int days) {
            fields.put("fromDate", UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), days));
            return this;
        }

        /** Registration validity end (days relative to now, negative = expired). */
        public Registration thruDaysFromNow(int days) {
            fields.put("thruDate", UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), days));
            return this;
        }

        public void create() {
            try {
                delegator.create(delegator.makeValue("PartyTaxAuthInfo", fields));
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot create registration", e);
            }
        }
    }

    /** Starts a tax registration of {@code partyId} with {@code authority}. */
    public Registration registration(String partyId, Authority authority) {
        return new Registration(partyId, authority);
    }

    // ------------------------------------------------------------------ store & catalog

    /** Builder for a store. */
    public final class Store {
        private final Map<String, Object> fields = new HashMap<>();
        private final String storeId = id("ST");

        Store() {
            fields.put("productStoreId", storeId);
            fields.put("storeName", "Airlift store " + storeId);
        }

        /** The organization paid for sales in this store (defaults to a fresh organization). */
        public Store payTo(String partyId) {
            fields.put("payToPartyId", partyId);
            return this;
        }

        /** Assign the store to the default store group (needed for store-group price lookups). */
        public Store inDefaultStoreGroup() {
            fields.put("primaryStoreGroupId", NA);
            return this;
        }

        /** Display prices VAT-inclusive using {@code authority}'s rates. */
        public Store vatDisplay(Authority authority) {
            fields.put("showPricesWithVatTax", "Y");
            fields.put("vatTaxAuthGeoId", authority.geoId());
            fields.put("vatTaxAuthPartyId", authority.partyId());
            return this;
        }

        /** Creates the store; returns its id. */
        public String create() {
            if (!fields.containsKey("payToPartyId")) {
                fields.put("payToPartyId", organization("Owner-" + storeId));
            }
            try {
                delegator.create(delegator.makeValue("ProductStore", fields));
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot create store", e);
            }
            return storeId;
        }
    }

    /** Starts a store definition. */
    public Store store() {
        return new Store();
    }

    /** Builder for a product. */
    public final class ProductBuilder {
        private final Map<String, Object> fields = new HashMap<>();
        private final String productId = id("PR");
        private String virtualParentId;

        ProductBuilder() {
            fields.put("productId", productId);
            fields.put("productTypeId", "FINISHED_GOOD");
            fields.put("internalName", "Airlift product " + productId);
            fields.put("isVariant", "N");
            fields.put("isVirtual", "N");
        }

        /** Whether the product is taxable (default: unset, which the system treats as taxable). */
        public ProductBuilder taxable(boolean flag) {
            fields.put("taxable", flag ? "Y" : "N");
            return this;
        }

        /** Leaves the taxable flag completely unset. */
        public ProductBuilder taxableUnset() {
            fields.remove("taxable");
            return this;
        }

        /** Marks this product a variant of {@code virtualProductId}. */
        public ProductBuilder variantOf(String virtualProductId) {
            fields.put("isVariant", "Y");
            this.virtualParentId = virtualProductId;
            return this;
        }

        /** Marks the product virtual (has variants). */
        public ProductBuilder virtual() {
            fields.put("isVirtual", "Y");
            return this;
        }

        /** Creates the product; returns its id. */
        public String create() {
            try {
                delegator.create(delegator.makeValue("Product", fields));
                if (virtualParentId != null) {
                    delegator.create(delegator.makeValue("ProductAssoc",
                            Map.of("productId", virtualParentId, "productIdTo", productId,
                                    "productAssocTypeId", "PRODUCT_VARIANT", "fromDate", past)));
                }
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot create product", e);
            }
            return productId;
        }
    }

    /** Starts a product definition. */
    public ProductBuilder product() {
        return new ProductBuilder();
    }

    /** A product category. Returns its id. */
    public String category() {
        String categoryId = id("CT");
        create("ProductCategory", "productCategoryId", categoryId,
                "categoryName", "Airlift category " + categoryId);
        return categoryId;
    }

    /** Puts {@code productId} into {@code categoryId}. */
    public void inCategory(String productId, String categoryId) {
        create("ProductCategoryMember", "productCategoryId", categoryId,
                "productId", productId, "fromDate", past);
    }

    /** Builder for a catalog price of a product, tagged with a tax authority. */
    public final class Price {
        private final Map<String, Object> fields = new HashMap<>();

        Price(String productId, Authority authority) {
            fields.put("productId", productId);
            fields.put("productPriceTypeId", "DEFAULT_PRICE");
            fields.put("productPricePurposeId", "PURCHASE");
            fields.put("currencyUomId", "USD");
            fields.put("productStoreGroupId", NA);
            fields.put("fromDate", past);
            fields.put("taxAuthGeoId", authority.geoId());
            fields.put("taxAuthPartyId", authority.partyId());
        }

        /** The net (tax-exclusive) price. */
        public Price price(String amount) {
            fields.put("price", dec(amount));
            return this;
        }

        /** The advertised tax-inclusive price. */
        public Price priceWithTax(String amount) {
            fields.put("priceWithTax", dec(amount));
            return this;
        }

        /** Whether the quoted price already contains the tax. */
        public Price taxInPrice(boolean flag) {
            fields.put("taxInPrice", flag ? "Y" : "N");
            return this;
        }

        public void create() {
            try {
                delegator.create(delegator.makeValue("ProductPrice", fields));
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot create price", e);
            }
        }
    }

    /** Starts a price definition for {@code productId} tagged with {@code authority}. */
    public Price price(String productId, Authority authority) {
        return new Price(productId, authority);
    }

    // ------------------------------------------------------------------ addresses & facilities

    /** Builder for a destination address (kept in memory; not persisted). */
    public final class Address {
        private final GenericValue value;

        Address() {
            value = delegator.makeValue("PostalAddress");
            value.set("contactMechId", id("AD"));
            value.set("address1", "1 Airlift Way");
            value.set("city", "Testville");
        }

        public Address country(String geoId) {
            value.set("countryGeoId", geoId);
            return this;
        }

        public Address state(String geoId) {
            value.set("stateProvinceGeoId", geoId);
            return this;
        }

        public Address county(String geoId) {
            value.set("countyGeoId", geoId);
            return this;
        }

        public Address postalCodeGeo(String geoId) {
            value.set("postalCodeGeoId", geoId);
            return this;
        }

        GenericValue toValue() {
            return value;
        }
    }

    /** Starts an address definition. */
    public Address address() {
        return new Address();
    }

    /** A facility (physical shop) whose configured address is {@code address}. Returns facility id. */
    public String facility(Address address) {
        String contactMechId = id("CM");
        create("ContactMech", "contactMechId", contactMechId, "contactMechTypeId", "POSTAL_ADDRESS");
        GenericValue postal = address.toValue();
        postal.set("contactMechId", contactMechId);
        try {
            delegator.create(postal);
        } catch (Exception e) {
            throw new IllegalStateException("fixture: cannot persist facility address", e);
        }
        String facilityId = id("F");
        create("Facility", "facilityId", facilityId, "facilityTypeId", "WAREHOUSE",
                "facilityName", "Airlift facility " + facilityId);
        create("FacilityContactMech", "facilityId", facilityId, "contactMechId", contactMechId,
                "fromDate", past);
        create("FacilityContactMechPurpose", "facilityId", facilityId, "contactMechId", contactMechId,
                "contactMechPurposeTypeId", "SHIP_ORIG_LOCATION", "fromDate", past);
        return facilityId;
    }

    // ------------------------------------------------------------------ invocation

    /** Builder for one tax-calculation request. */
    public final class Calc {
        private final Map<String, Object> ctx = new HashMap<>();
        private final List<GenericValue> products = new ArrayList<>();
        private final List<BigDecimal> amounts = new ArrayList<>();
        private final List<BigDecimal> prices = new ArrayList<>();
        private final List<BigDecimal> quantities = new ArrayList<>();
        private final List<BigDecimal> shipping = new ArrayList<>();
        private boolean anyQuantity;
        private boolean anyShipping;

        /** Sell through this store. */
        public Calc store(String productStoreId) {
            ctx.put("productStoreId", productStoreId);
            return this;
        }

        /** No store: sell on behalf of this organization directly. */
        public Calc payTo(String partyId) {
            ctx.put("payToPartyId", partyId);
            return this;
        }

        /** The purchasing party billed for the order. */
        public Calc billTo(String partyId) {
            ctx.put("billToPartyId", partyId);
            return this;
        }

        /** Ship to this destination address. */
        public Calc shipTo(Address address) {
            ctx.put("shippingAddress", address.toValue());
            return this;
        }

        /** Face-to-face sale at this facility (no shipping address). */
        public Calc atFacility(String facilityId) {
            ctx.put("facilityId", facilityId);
            return this;
        }

        /**
         * Adds an order line: unit price and quantity as decimal strings.
         * The line amount defaults to price x quantity.
         */
        public Calc item(String productId, String unitPrice, String quantity) {
            BigDecimal price = dec(unitPrice);
            BigDecimal qty = dec(quantity);
            return itemWithAmount(productId, unitPrice, quantity,
                    price.multiply(qty).toPlainString());
        }

        /** Adds an order line with an explicit line amount (e.g. after discounts). */
        public Calc itemWithAmount(String productId, String unitPrice, String quantity, String lineAmount) {
            try {
                GenericValue product = productId == null ? null
                        : EntityQuery.use(delegator).from("Product")
                                .where("productId", productId).queryOne();
                products.add(product);
            } catch (Exception e) {
                throw new IllegalStateException("fixture: cannot load product " + productId, e);
            }
            prices.add(dec(unitPrice));
            BigDecimal qty = dec(quantity);
            quantities.add(qty);
            if (qty != null) {
                anyQuantity = true;
            }
            amounts.add(dec(lineAmount));
            shipping.add(null);
            return this;
        }

        /** Sets the shipping charge of the most recently added line. */
        public Calc lineShipping(String amount) {
            shipping.set(shipping.size() - 1, dec(amount));
            anyShipping = true;
            return this;
        }

        /** Order-level freight charge (split across lines by value weight). */
        public Calc orderShipping(String amount) {
            ctx.put("orderShippingAmount", dec(amount));
            return this;
        }

        /** Order-level promotions amount (usually negative). */
        public Calc orderPromotions(String amount) {
            ctx.put("orderPromotionsAmount", dec(amount));
            return this;
        }

        private Map<String, Object> buildContext() {
            ctx.put("itemProductList", products);
            ctx.put("itemAmountList", amounts);
            ctx.put("itemPriceList", prices);
            if (anyQuantity) {
                ctx.put("itemQuantityList", quantities);
            }
            if (anyShipping) {
                ctx.put("itemShippingList", shipping);
            }
            return ctx;
        }

        /** Runs the calculation, failing the test on a service error. */
        public TaxCalcResult run() {
            try {
                Map<String, Object> result = dispatcher.runSync("calcTax", buildContext());
                if (ServiceUtil.isError(result)) {
                    throw new IllegalStateException("calcTax returned error: "
                            + ServiceUtil.getErrorMessage(result));
                }
                return new TaxCalcResult(result);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("calcTax failed: " + e.getMessage(), e);
            }
        }

        /** Runs the calculation expecting a service error; returns the error message. */
        public String runExpectingError() {
            try {
                Map<String, Object> result = dispatcher.runSync("calcTax", buildContext());
                if (ServiceUtil.isError(result)) {
                    return ServiceUtil.getErrorMessage(result);
                }
                throw new IllegalStateException("calcTax unexpectedly succeeded");
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }

    /** Starts a tax-calculation request. */
    public Calc calcTax() {
        return new Calc();
    }

    /** Result of a display-price tax calculation. */
    public static final class DisplayResult {
        private final BigDecimal taxTotal;
        private final BigDecimal taxPercentage;
        private final BigDecimal priceWithTax;

        DisplayResult(Map<String, Object> result) {
            this.taxTotal = (BigDecimal) result.get("taxTotal");
            this.taxPercentage = (BigDecimal) result.get("taxPercentage");
            this.priceWithTax = (BigDecimal) result.get("priceWithTax");
        }

        public BigDecimal taxTotal() {
            return taxTotal;
        }

        public BigDecimal taxPercentage() {
            return taxPercentage;
        }

        public BigDecimal priceWithTax() {
            return priceWithTax;
        }
    }

    /**
     * Runs the display-price tax calculation (store must be VAT-display configured).
     * Quantity and shippingPrice may be null.
     */
    public DisplayResult calcTaxForDisplay(String productStoreId, String productId,
            String basePrice, String quantity, String shippingPrice) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("productStoreId", productStoreId);
        ctx.put("productId", productId);
        ctx.put("basePrice", dec(basePrice));
        if (quantity != null) {
            ctx.put("quantity", dec(quantity));
        }
        if (shippingPrice != null) {
            ctx.put("shippingPrice", dec(shippingPrice));
        }
        try {
            Map<String, Object> result = dispatcher.runSync("calcTaxForDisplay", ctx);
            if (ServiceUtil.isError(result)) {
                throw new IllegalStateException("calcTaxForDisplay returned error: "
                        + ServiceUtil.getErrorMessage(result));
            }
            return new DisplayResult(result);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("calcTaxForDisplay failed: " + e.getMessage(), e);
        }
    }
}
