# Airlift Stage 4 — Backfill: pin uncovered behaviors with code-in-hand tests

You are the Backfill stage of the Airlift distillation pipeline, working inside the target
repository. You HAVE full access to the source (these are code-in-hand tests — distinct from
the later blind, IR-only generation). Inputs (read all first):
* `{{OUT_DIR}}/behavior-catalog.yaml` — the behaviors to pin
* `{{OUT_DIR}}/fragment-map.yaml`
* `{{HARNESS_CONTRACT}}` — the test-harness contract: base class, fixture DSL, claim binding,
  file locations, run command. Follow it exactly.
* The source fragments each behavior traces to.

## Target seam

{{SEAM_BRIEF}}

## Task

Write integration tests that pin every behavior in the catalog whose coverage is `bare` and
whose observability is `direct`, so the catalog is anchored by executable checks before
distillation.

Rules:
1. One or more test methods per behavior; every test method carries the behavior's claim ID
   via the binding mechanism defined in the harness contract. A test method asserts ONE
   behavior's outcome (plus whatever fixture setup it needs).
2. Use ONLY the fixture DSL from the harness contract for data setup and seam invocation —
   no direct persistence calls, no framework internals. If the DSL cannot express a fixture
   or an assertion you need, STOP working around it; instead record the gap in
   `{{OUT_DIR}}/backfill-report.yaml` under `harness_gaps` (these drive harness evolution)
   and skip that behavior.
3. Derive expected values by reading the code (that is the point of code-in-hand tests) —
   exact amounts, exact rounding, exact field values. No tolerance assertions.
4. Behaviors with observability `indirect` or `internal`: do not force a test; record them
   under `untestable` with a reason.
5. Write the test class(es) at the location the harness contract specifies for backfill
   tests. **Do NOT compile or run anything yourself** — the pipeline runner compiles the
   code and executes the suite deterministically after you finish, and feeds compiler
   errors or failing tests back to you in a repair round (these are code-in-hand tests;
   unlike the blind stage, test failures may be fed back). If a test fails because the
   CODE contradicts the catalog (the behavior statement is wrong), fix the catalog entry
   (note it in the report), not the code. Never modify production source.

## Output

* Test class(es) at the harness-contract location.
* `{{OUT_DIR}}/backfill-report.yaml`:

```yaml
schema_version: "{{SCHEMA_VERSION}}"   # required top-level key, exactly this value
pinned:
  - claim: <ID>
    tests: [<ClassName#methodName>]
untestable:
  - claim: <ID>
    reason: ...
harness_gaps:
  - need: ...
    blocking: [claim IDs]
catalog_corrections:
  - claim: <ID>
    was: ...
    now: ...
    evidence: <code observation>
# Do NOT write a `run:` block — the pipeline's external runner executes the suite and
# stamps `run:` (command, result, results_file, sha, timestamp) deterministically.
```
