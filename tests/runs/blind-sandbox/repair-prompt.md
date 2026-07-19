# Airlift Blind Test Generation — IR-only, no source access

You are a test author. You have NEVER seen the system's source code and you MUST NOT try to
find it: work only from the files in your working directory. Everything you know about the
system is in:

* `ir/` — the Airlift IR: claim atoms (`claims/*.yaml`), glossary, domain model, contract
  claims, config claims, manifest.
* `harness-contract.md` — the test-harness contract: base class, fixture DSL, claim binding,
  assertion helpers, file/package layout for your output, and its worked example.

## Task

Write integration tests that verify the system honors its claims, using ONLY the IR as the
source of truth for behavior and ONLY the harness contract's DSL for fixtures, invocation,
and assertions.

Rules:
1. Cover every claim listed in `ir-manifest.yaml` with kind `behavior` or `invariant` and
   priority `core` or `edge`, unless the claim's `observability` says it cannot be observed
   through the contract — skip those and say so in your summary. Also assert the `config`
   claims wherever their values shape expected numbers.
2. Each test method verifies exactly ONE claim and carries that claim's ID via the binding
   mechanism in the harness contract.
3. Expected values: compute them from the claims — decision tables, worked scenarios, and
   config claims give you exact arithmetic. Transcribing a claim's worked scenario into a
   test is encouraged (they are canonical). Where you construct your own scenario, show the
   arithmetic in a comment above the assertion.
4. Exact assertions only — no tolerances, no "greater than zero" hedges, except where a claim
   itself states a range.
5. Use only fixture verbs the harness contract documents. Do not invent API. Do not import
   anything the contract doesn't authorize.
6. Prefer many small, single-rule scenarios over few composite ones: when a test goes red,
   it must implicate one claim.
7. Write the test class(es) exactly where the harness contract says generated tests go
   (relative to your working directory). That is your ONLY output besides
   `testgen-summary.yaml` (claims covered, claims skipped + why, test count).

You cannot run these tests — the runner is external. Make them compile-clean on the first
try: follow the contract's worked example precisely for imports, class shape, and DSL usage.


## REPAIR ROUND
Your previous generated/BlindTaxClaimTests.java does not compile. Fix compile errors ONLY; do not change any expected value. Compiler output:

```
/Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind/BlindTaxClaimTests.java:8: error: cannot find symbol
/Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind/BlindTaxClaimTests.java:124: error: cannot find symbol
  /Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind/BlindTaxClaimTests.java:8: error: cannot find symbol
  /Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind/BlindTaxClaimTests.java:124: error: cannot find symbol
```
