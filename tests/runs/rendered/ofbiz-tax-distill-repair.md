You are the Distill stage of the Airlift distillation pipeline in repair mode. Your IR at
/Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/ir/ failed deterministic validation:

```
ir: TAX.DISPLAY.FINAL-ROUNDING: term 'final-precision' not in glossary
ir: TAX.EXEMPT.EFFECTIVE-INFO: term 'customer-tax-id' not in glossary
ir: TAX.ORDER.VALUE-WEIGHT: term 'order-value' not in glossary
ir: TAX.VAT.PRICE-GROUP-SCOPE: term 'tax-authority-party' not in glossary
```

Fix in place: read glossary.yaml and the four claim files, then EITHER add the missing terms
to glossary.yaml (id `term.<slug>`, canonical, definition, not, aliases — consistent with the
existing entries and with how the claims use them) OR, where an existing glossary term
already names the same concept, change the claim's `terms:` entry to the existing term.
Choose whichever keeps the vocabulary canonical (no near-duplicate terms). Also verify the
claims' behavior bodies use the resolved term names. Do not change anything else.

Finally re-read /Volumes/Dancer/Develop/AIRLIFT/airlift/out/ofbiz-tax/ir/ir-manifest.yaml and
confirm it is complete per the IR schema at
/Volumes/Dancer/Develop/AIRLIFT/airlift/ir-spec/IR-SCHEMA.md (claim index with id/kind/title/
status/priority, pinned_claims, unpinned_claims, descoped, catalog_deltas, source binding
commit) — your run was interrupted while writing it; complete it if truncated.
