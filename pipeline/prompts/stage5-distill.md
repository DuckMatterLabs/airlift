# Airlift Stage 5 — Distill: emit the business-logic IR

You are the Distill stage of the Airlift distillation pipeline, working inside the target
repository. Inputs (read ALL before writing anything):

* `{{IR_SPEC}}/IR-SCHEMA.md` — the normative IR schema. Follow it exactly.
* `{{OUT_DIR}}/fragment-map.yaml`
* `{{OUT_DIR}}/coverage-gaps.yaml`
* `{{OUT_DIR}}/behavior-catalog.yaml`
* `{{OUT_DIR}}/backfill-report.yaml` — including catalog corrections discovered while pinning
* `{{HARNESS_CONTRACT}}` — the fixture vocabulary your domain model must align with.
* The source fragments themselves (re-read them; precision comes from code, not the catalog).

## Target seam

{{SEAM_BRIEF}}

## Task

Emit the complete Airlift IR for the seam into `{{OUT_DIR}}/ir/`, per the schema:

1. **Every behavior** in the catalog with priority `core` or `edge` becomes a claim atom in
   `claims/<ID>.yaml`. `secondary` behaviors either become claims or are listed in
   `ir-manifest.yaml` under `descoped` with a reason. Behaviors may be merged or split
   relative to the catalog when that yields crisper claims — record any such change in the
   manifest under `catalog_deltas`. Apply the backfill report's `catalog_corrections`.
2. **Constrained Gherkin** per schema section 4. Every noun must be a glossary term; define
   every term you use in `glossary.yaml`. No implementation identifiers anywhere in
   `behavior:` bodies (class, entity, field, service, config-key names are architecture —
   they belong in traceability and the architecture stub, never in claims).
3. **Scenarios with exact numbers**: for every `core` claim provide at least one worked
   numeric scenario (`scenarios:`) whose arithmetic you have verified by hand against the
   code's actual precision/rounding rules. Emit those precision/rounding rules as `config`
   claims (read the actual configuration to get the values; cite files in traceability).
4. **Contract claim(s)**: describe the seam's request/response in domain vocabulary: what a
   caller supplies and what comes back, field by field, in glossary terms. This is what makes
   blind test generation possible.
5. **Domain model** (`domain-model.yaml`): the conceptual entities claims quantify over,
   their attributes (with units/enumerations), and for each concept the fixture verbs from
   the harness contract that construct it. Every `Given` in every claim must be expressible
   via these verbs.
6. **Traceability** (`traceability.yaml`): every claim lists its fragment IDs; every fragment
   carries file/symbol/lines at the current commit. Also ANNOTATE the source: insert a
   comment line (in the file's comment syntax) `AIRLIFT: <fragment-id>` at the first line of
   each business-logic or fused fragment in the primary source file(s) — comment-only change;
   do not alter any code line; the file must still compile.
7. **Provenance**: mine version-control history (`git log --follow`, `git log -S<token>`) for
   the claims the seam brief names as provenance-critical, plus any claim whose intent is
   unclear from code alone: when introduced, what changed semantically since. Emit
   `provenance/<CLAIM-ID>.yaml` per schema. Mark inference honestly (`confidence: inferred`).
8. **Architecture stub** (`architecture/`): ADR-style notes (markdown) for the seam's
   architectural boundaries — at minimum: (a) the public-contract boundary pattern, (b) the
   persistence pattern, (c) the integration-test harness pattern — each referencing a
   mustache-parameterized pattern snippet in `architecture/patterns/`. Keep it a stub: enough
   to show where architecture knowledge lives so claims don't have to carry it.
9. **Manifest** (`ir-manifest.yaml`): claim index (id, kind, title, status, priority),
   `pinned_claims` (claims with a green backfill test, from the backfill report),
   `unpinned_claims`, `descoped`, `catalog_deltas`, source binding commit.

## Discipline

* Classify, never discard: anything you drop must appear in `descoped` with a reason.
* The IR must stand alone: a reader with the IR and the harness contract — but no access to
  this repository — must be able to write concrete, runnable tests with exact expected
  numbers. Read your own output with that reader's eyes before finishing.
* Claim IDs are forever. Choose them carefully.

Write only under `{{OUT_DIR}}/ir/` plus the comment annotations in the primary source
file(s).
