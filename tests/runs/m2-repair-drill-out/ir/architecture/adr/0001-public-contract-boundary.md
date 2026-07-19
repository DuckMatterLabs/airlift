# ADR 0001 — Public-contract boundary (service-engine seam)

## Status
Accepted (descriptive; reverse-engineered).

## Context
The tax-calculation logic is a plain Java class, but its only wiring to the rest of the system
is through the XML **service engine**. Two services form the seam's public contract:

- `calcTax` → the order-level tax calculation (maps to `rateProductTaxCalc`).
- `calcTaxForDisplay` → the single-product display price-with-tax (maps to
  `rateProductTaxCalcForDisplay`).

Their request/response shapes are declared as service **interfaces** in
`applications/accounting/servicedef/services_tax.xml` (fragments F-STAX-001..004). Callers never
touch the Java class directly; they invoke a named service with a Map of parameters and receive
a Map of results (notably `orderAdjustments` / `itemAdjustments`, a list of `OrderAdjustment`
value objects).

## Decision / Consequence
The seam's testable surface is the service contract, not the Java signature. Domain claims
`TAX.CONTRACT.CALC-ORDER-TAX` and `TAX.CONTRACT.DISPLAY-PRICE-WITH-TAX` describe this boundary in
glossary vocabulary. The output shape (`OrderAdjustment`) is the single response contract:
adjustment type, amount, exempt-amount, included-tax, rate percent, jurisdiction geo, authority
party, primary geo, customer tax id.

## Pattern
`patterns/service-contract.mustache`
