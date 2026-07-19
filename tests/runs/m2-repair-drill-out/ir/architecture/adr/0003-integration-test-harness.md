# ADR 0003 — Integration-test harness pattern

## Status
Accepted (descriptive).

## Context
The seam is exercised end-to-end through a fixture DSL (the harness contract) that builds geos,
tax authorities and rates, parties and registrations, stores, products and prices, addresses and
facilities, then invokes `calcTax()` / `calcTaxForDisplay()` and asserts on the returned
adjustments. The DSL verbs are the *only* architecture the blind test generator sees; the
domain-model's `fixture_verbs` are aligned to them one-for-one.

## Decision / Consequence
Every `Given` in every claim is expressible via harness verbs (fx.country/state/county/containedIn,
taxAuthority/rate/inheritExemption, organization/customer/groupRollup, registration, store/product/
category/price, address/facility, calcTax builder). Assertions use `assertDecimal` on adjustment
accessors (type/amount/exemptAmount/includedTax/ratePercent/...). Worked numeric scenarios in the
core claims are written to be reproduced directly through this harness.

## Pattern
`patterns/harness-fixture.mustache`
