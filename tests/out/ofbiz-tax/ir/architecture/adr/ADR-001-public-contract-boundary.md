# ADR-001: Public-contract boundary — XML service definitions over Java implementations

Status: descriptive (records the existing architecture; not a new decision)

## Context

The sales-tax seam's business logic lives in a single Java class
(`org.apache.ofbiz.accounting.tax.TaxAuthorityServices`) with two static entry-point methods.
Neither method is called directly by application code. Instead they are wired to the outside
world exclusively through XML *service definitions* in
`applications/accounting/servicedef/services_tax.xml`:

- `calcTax` (implements `calcTaxInterface`) → `rateProductTaxCalc`
- `calcTaxForDisplay` (implements `calcTaxTotalForDisplayInterface`) → `rateProductTaxCalcForDisplay`

The XML declares the `IN`/`OUT` attribute contract (parameter names, types, optionality). The
OFBiz service engine performs coercion and dispatch: a caller invokes a *named service* with a
`Map<String,Object>` context and receives a result `Map`; the engine routes to the Java method
named in the `location`/`invoke` attributes.

## Decision / boundary

The unit of external truth is the **service name and its attribute contract**, not the Java
signature. The contract claims TAX.CONTRACT.CALC-TAX and TAX.CONTRACT.CALC-TAX-FOR-DISPLAY
describe this boundary in domain vocabulary; the field-by-field wiring (which XML attribute maps
to which context key) is architecture and is captured only here and in traceability
(fragments F-STAX-001..004, F-TAS-003, F-TAS-014).

## Consequences for testing

Tests exercise the seam by invoking the *service* through the dispatcher, supplying the context
map the interface declares and asserting over the returned `orderAdjustments` /
`itemAdjustments` (order path) or `taxTotal` / `taxPercentage` / `priceWithTax` (display path).
The concrete invocation shape is the mustache pattern
`architecture/patterns/service-contract.mustache`.

## Pattern

See `architecture/patterns/service-contract.mustache`.
