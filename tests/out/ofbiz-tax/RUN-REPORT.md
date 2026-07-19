# Airlift Goal 1 — run report (target: ofbiz-tax)

Date: 2026-07-19. Execution harness for every pipeline stage and every proof agent:
**GitHub Copilot CLI 1.0.71 + Anthropic Opus 4.8** (BYOK, `COPILOT_MODEL=claude-opus-4-8`).
Target: Apache OFBiz `TaxAuthorityServices` (sales-tax seam), OFBiz branch `airlift`.

## Distillation passes (all Copilot+Opus, all validated deterministically)

| Pass | Output | Result |
|---|---|---|
| 1 Map | `fragment-map.yaml` | 79 fragments / 8 files; 20 fused seams flagged; total line coverage of the 772-line class verified; passed validation attempt 1 |
| 2 Test landscape | `coverage-gaps.yaml` | zero native OFBiz tests touch the seam (evidence-backed, 9 searches); 15 behavior fragments bare |
| 3 Catalog | `behavior-catalog.yaml` | 30 behaviors / 7 areas; 2 honestly marked unobservable-via-contract; passed attempt 1 |
| 4 Backfill | `backfill-report.yaml` + `BackfillTaxTests` | 19 code-in-hand tests pinning all 15 bare direct behaviors; green; zero harness gaps |
| 5 Distill | `ir/` | 34 claims (30 behavior, 2 contract, 2 config), 75-term glossary, domain model + fixture vocabulary, traceability with 30 in-source fragment annotations, git provenance for exemption + threshold claims, architecture stub (3 ADRs + 3 mustache patterns). One interrupted-run repair round (4 missing glossary terms) |

## IR feedback loop (the method working as designed)

First blind trial: 30/35 green. The 5 reds decomposed into:
* 2 × harness defect (variant fixture FK ordering) — fixed in the harness, not the IR;
* 3 × IR ambiguity — a blind reader could construct invalid scenarios (county-only address
  for containment; store-VAT-display mistaken for the tax-inclusive-price trigger).
  Repaired the CLAIMS (clarity, matching the code — never the tests); reran blind
  generation FRESH from the repaired IR. No genuine extraction errors found.

## Exit criteria

| Criterion | Verdict |
|---|---|
| E1 blind IR-only testgen, green vs real code | **PASSED** — 35 tests / 35 green (agent sandboxed: no shell, no web, no repo file access — verified) |
| E2 planted bugs caught | **PASSED — 7/7 caught**, each naming the right claims; includes the ≤→< boundary bug and the audit-trail (exempt-amount) bug |
| E3 behavior-preserving refactor stays green | **PASSED** — Copilot+Opus extracted 10 business-named methods from the ~300-line `getTaxAdjustments` monolith (675-line diff, traceability annotations preserved); 35/35 blind tests stayed green (`tests/out/ofbiz-tax/e3/`) |
| E4 flipped exemption rule caught AND named | **PASSED** — suite red; spine named TAX.EXEMPT.ZEROING, TAX.EXEMPT.GROUP-ROLLUP, TAX.EXEMPT.JURISDICTION-INHERITANCE, TAX.EXEMPT.EFFECTIVE-INFO |

E2 detail (violated claims per mutation) — `tests/runs/e2-summary.txt`:
* M1 exempt-flip → all four TAX.EXEMPT claims
* M2 min-price boundary → TAX.RATE.THRESHOLD-MIN-ITEM-PRICE (exactly)
* M3 item amount untaxed → TAX.CALC.* + downstream
* M4 shipping untaxed → TAX.RATE.SHIPPING-TAXABLE + TAX.ORDER.FREIGHT-SPLIT
* M5 rollup direction → TAX.EXEMPT.GROUP-ROLLUP (exactly)
* M6 inheritance cut → TAX.EXEMPT.JURISDICTION-INHERITANCE (exactly)
* M7 exempt-amount zeroed → TAX.EXEMPT.ZEROING (+ rollup/inheritance recording)

## Artifacts

* IR: `tests/out/ofbiz-tax/ir/` (claims, glossary, domain model, traceability, provenance,
  architecture stub)
* Blind suite: OFBiz `applications/accounting/src/test/java/.../tax/test/blind/` (35 tests)
* Backfill suite: `.../tax/test/backfill/` (19 tests)
* Harness: `tests/targets/ofbiz-tax/harness/` (DSL, contract, spine reporter)
* Logs & spine reports: `tests/runs/`
