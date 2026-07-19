# ADR 0002 — Persistence pattern (entity-engine query construction fused with rules)

## Status
Accepted (descriptive; reverse-engineered).

## Context
Business rules in this seam are *fused* with persistence: matching a rate rule, scoping to a
store, matching a product category, and applying thresholds are all expressed as **entity-engine
condition lists** (`EntityCondition`) built inline and run via `EntityQuery`. Examples:
store-scope and authority conditions (F-TAS-031/032), category-match condition (F-TAS-052),
threshold main condition (F-TAS-036), price resolution (F-TAS-049). The entity definitions those
queries quantify over live in `*-entitymodel.xml` (fragments F-ACCTEM-*, F-PRODEM-*, F-PARTYEM-*,
F-COMMONEM-*, F-ORDEREM-*).

## Decision / Consequence
The domain-model concepts (`rate-rule`, `taxing-jurisdiction`, `product-category`, `tax-registration`)
are the conceptual projections of these entities; the query predicates encode the matching rules.
Claims speak of the *rule* ("a rate rule under the resolved jurisdiction's tax authority"); the
*mechanism* (condition-list construction) is captured only here and in traceability's `fused`
fragments. A test harness constructs fixtures through domain verbs and never writes entity rows
directly in the claim vocabulary.

## Pattern
`patterns/entity-query-condition.mustache`
