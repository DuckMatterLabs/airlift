You are the Distill stage of the Airlift distillation pipeline in repair mode, working in
the Apache OFBiz repository. The IR at /Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/ir/
passed structural validation, but a BLIND test-generation trial exposed ambiguities: a
reader with only the IR constructed scenarios that contradict the code. The code is correct;
the claims were not precise enough. Evidence (how a blind reader went wrong):

1. TAX.JURIS.CONTAINMENT — the blind reader built a destination address naming ONLY a county
   (no state/country/postal code). The system rejects such an address per
   TAX.JURIS.ADDRESS-REQUIRED before containment can apply. Repair CONTAINMENT so its Given
   and its worked scenario can only be read as: the destination address itself satisfies the
   address requirement (e.g. names a state), and containment then extends matching to an
   ENCLOSING region's authority (e.g. a country-level authority applying to a state-named
   address). Re-read the source (getTaxAuthorities + the address guard in rateProductTaxCalc,
   see fragment map) to confirm exactly which address facts license containment, including
   whether a county named IN ADDITION to a state participates.

2. TAX.VAT.PRICE-INCLUSIVE and TAX.VAT.DISCOUNT-CORRECTION — the blind reader believed
   tax-inclusive treatment is triggered by the STORE's VAT-display setting; it never created
   the product's tax-inclusive catalog price, so the system produced a plain added-on tax
   line. Re-read the source (the product-price resolution and VAT branch in
   getTaxAdjustments) and repair BOTH claims so the Given names the actual trigger — a
   catalog price record for the product in the taxing jurisdiction marked tax-inclusive —
   in fixture-expressible vocabulary, and each worked scenario's `given:` includes that
   catalog-price fixture explicitly (net price, tax-inclusive flag, jurisdiction). Make the
   store's VAT-display setting's true role unambiguous: state in the DISPLAY-area claims
   that it governs only the display-price path, and if any wording in the VAT-area claims
   could be read as store-triggered, remove it. Check the sibling claims TAX.VAT.PRICE-CORRECTION,
   TAX.VAT.VARIANT-PRICE and TAX.VAT.PRICE-GROUP-SCOPE for the same ambiguity and repair
   them the same way.

3. Update /Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/ir/domain-model.yaml so the
   tax-inclusive-price concept maps explicitly to its fixture verb chain per the harness
   contract at /Volumes/Dancer/Develop/AIRLIFT/airlift/targets/ofbiz-tax/harness/harness-contract.md
   (price(product, authority) ... taxInPrice(true) ... create()), and the containment
   concept's fixture mapping shows the address must still name a state/country.

Discipline: you are clarifying claims to match the CODE, not to match any test. Keep claim
IDs stable. Keep glossary consistent (add terms if needed). Do not touch any file outside
the ir/ directory.
