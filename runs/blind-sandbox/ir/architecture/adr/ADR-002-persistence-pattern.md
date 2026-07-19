# ADR-002: Persistence pattern — entity engine and condition-as-rule

Status: descriptive

## Context

The seam never touches SQL or JDBC directly. All persistence goes through the OFBiz *entity
engine*: entity definitions in `*-entitymodel.xml` files declare logical entities (TaxAuthority,
TaxAuthorityRateProduct, PartyTaxAuthInfo, TaxAuthorityAssoc, ProductPrice, PostalAddress, Geo,
GeoAssoc, OrderAdjustment, ...), and code reads/writes them via `EntityQuery` and
`GenericValue`.

Two architecturally load-bearing facts:

1. **The output shape is an entity value.** Each returned tax line is a `GenericValue` of type
   `OrderAdjustment` (F-ORDEREM-001), populated in memory and returned — not persisted by this
   code. The domain-model concept `tax-adjustment` is this shape; its fields map to
   OrderAdjustment columns (`amount`, `amountAlreadyIncluded`, `exemptAmount`,
   `sourcePercentage`, `orderAdjustmentTypeId`, `primaryGeoId`, `taxAuthGeoId`,
   `taxAuthPartyId`, `customerReferenceId`, `overrideGlAccountId`, `comments`).

2. **Rule matching is expressed as a query predicate.** Rate-rule selection (store scope,
   authority set, product-category membership, threshold minima, shipping/promotion
   taxability) is assembled into one `EntityCondition` over `TaxAuthorityRateProduct` and
   executed as a single ordered, date-filtered lookup (`.filterByDate()`,
   `orderBy("minItemPrice","minPurchase","fromDate")`). Business rules and persistence
   queries are the same tissue — this is why claims like TAX.RATE.* quantify over rate-rule
   attributes rather than over query mechanics.

## Decision / boundary

Claims describe *what data configuration produces what outcome*; the entity/field names and the
condition-construction mechanics are architecture, captured here and in traceability only. Date
effectivity (`fromDate`/`thruDate` with `filterByDate`) is the standard entity-engine idiom
behind every "currently-effective" glossary phrasing.

## Consequences for testing

Fixtures are built as entity rows via the harness fixture verbs (see domain-model.yaml). A test
seeds TaxAuthority / TaxAuthorityRateProduct / PartyTaxAuthInfo / ProductPrice / PostalAddress /
Geo(+GeoAssoc) rows, then invokes the service. See
`architecture/patterns/entity-query.mustache`.

## Pattern

See `architecture/patterns/entity-query.mustache`.
