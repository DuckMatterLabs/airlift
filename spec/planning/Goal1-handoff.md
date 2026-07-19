# Airlift — Goal 1 handoff

Status date: **2026-07-19**. Audience: a future Claude/agent session (or human) continuing
this work with no memory of how it was built. (Convention: each completed goal produces a
`Goal<N>-handoff.md` in `spec/planning/`; this is the first.)

## 1. Where things stand

**Goal 1 is COMPLETE** (per `spec/planning/Airlift-goal.md` + `spec/planning/goals.md`): the target-agnostic
distillation pipeline and Airlift IR exist, and were proven end-to-end on the required
execution harness — **GitHub Copilot CLI (`copilot -p`) with Anthropic Opus 4.8 via BYOK**
— against OFBiz `TaxAuthorityServices.rateProductTaxCalc` (sales-tax / tax-exemption seam).

All four exit criteria passed (details + evidence pointers in `out/ofbiz-tax/RUN-REPORT.md`):

| Exit | Verdict |
|---|---|
| E1 blind IR-only test generation, green vs real code | PASSED — 35/35 (sandboxed agent: no shell, no web, no repo file access — empirically verified) |
| E2 planted bugs caught by blind suite | PASSED — 7/7, correct claims named each time |
| E3 behavior-preserving refactor stays green | PASSED — 675-line diff, 10 extracted methods, 35/35 green |
| E4 flipped tax-exemption rule caught AND named | PASSED — spine named the four TAX.EXEMPT claims |

The one mid-run course correction that shapes everything: **the pipeline and IR are
target-agnostic by user directive.** OFBiz is only the first target plugin. Never put
target-specific content in `pipeline/prompts/` or `ir-spec/` — it goes in `targets/<name>/`.

## 2. Repo map (what each component is)

See `README.md` for the component descriptions. Quick index:

* `ir-spec/IR-SCHEMA.md` — normative IR schema (claim atoms, constrained Gherkin, glossary,
  traceability, spine, architecture/provenance stubs).
* `pipeline/` — generic engine: `prompts/*.md` (5 stages + blind-testgen + refactor, all
  `{{PLACEHOLDER}}`-parameterized), `lib.sh` (Copilot BYOK wrapper `run_copilot`),
  `render.py`, `validate.py` (per-stage deterministic validators), `run-pipeline.sh`
  (render → run → validate, ≤3 attempts with repair prompts).
* `targets/ofbiz-tax/` — target plugin: `target.env` (all paths/commands), `seam.md`
  (seam brief injected into prompts), `harness/` (fixture DSL `TaxFixture`, base case,
  `@AirliftClaim` annotation, `TaxCalcResult`, smoke tests, testdef suites, `install.sh`,
  `run-tests.sh`, `report-claims.py` = the spine reporter, `harness-contract.md` = the
  mechanics-only doc agents write tests against), `mutations/mutations.py` (7 planted bugs).
* `out/ofbiz-tax/` — pipeline products: `fragment-map.yaml`, `coverage-gaps.yaml`,
  `behavior-catalog.yaml`, `backfill-report.yaml`, `ir/` (34 claims, glossary 75 terms,
  domain-model, traceability, provenance/, architecture/), `e3/` (refactor artifacts),
  `RUN-REPORT.md` (the full narrative), `testgen-summary` lives in the blind sandbox copy.
* `exit/run-e1-blind.sh … run-e4-flip.sh` — the four proof runners.
* `runs/` — logs, rendered prompts, spine reports (`e1-spine.txt`, `e2-summary.txt`,
  `e2-*-spine.txt`, `e4-spine.txt`), blind sandbox (`runs/blind-sandbox/`).
* `.venv/` — python venv with PyYAML (validators/reporters use `./.venv/bin/python`).

**Outside this repo:**

* `/Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework` — OFBiz clone, branch **`airlift`**
  (local only). Contains: harness under
  `applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/`
  (+ `backfill/BackfillTaxTests.java` 19 tests, `blind/BlindTaxClaimTests.java` 35 tests),
  suite registrations in `applications/accounting/ofbiz-component.xml`, testdefs
  `airliftsmoketests/airliftbackfilltests/airliftblindtests.xml`, and 30 comment-only
  `// AIRLIFT: F-TAS-NNN` traceability annotations in `TaxAuthorityServices.java`.
  Commits on this branch are the mutation-revert baseline (`git checkout --` relies on them).
  Working tree was left CLEAN.
* H2 database at `ofbiz-framework/runtime/data/h2/` — loaded with
  `readers=seed,seed-initial,ext,ext-test` ONLY. **Never run `loadAll` / no-arg
  `--load-data`**: the demo tier ships live tax data (catch-all `_NA_/_NA_` rate rule 9000,
  store-null CA/NY/TX/UT rates, exempt demo parties) that bleeds into every fixture scenario.
* Memory file (Claude sessions): `airlift-goal1-pipeline.md` in the project memory dir.

## 3. How to run everything

```bash
cd /Volumes/Dancer/Develop/AIRLIFT/airlift

# Full distillation (stages: map testscape catalog backfill distill | all)
./pipeline/run-pipeline.sh targets/ofbiz-tax all

# Exit proofs (E1 must run first — it generates + installs the blind suite)
./exit/run-e1-blind.sh
./exit/run-e2-mutations.sh
./exit/run-e4-flip.sh
./exit/run-e3-refactor.sh

# Harness alone
./targets/ofbiz-tax/harness/install.sh          # idempotent copy into OFBiz
./targets/ofbiz-tax/harness/run-tests.sh airliftsmoke|airliftbackfill|airliftblind
./.venv/bin/python targets/ofbiz-tax/harness/report-claims.py \
    --results ../ofbiz-framework/runtime/logs/test-results/airliftblind.xml \
    --tests-dir ../ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind \
    --ir out/ofbiz-tax/ir
```

Copilot BYOK env is set inside `pipeline/lib.sh:airlift_env` (key read from `.env`;
`COPILOT_MODEL=claude-opus-4-8`). One-time DB setup if H2 is ever wiped:
`./gradle/init-gradle-wrapper.sh && ./gradlew generateSecretKeys "ofbiz --load-data readers=seed,seed-initial,ext,ext-test"`.

## 4. Gotchas that cost time (don't rediscover these)

* `pipeline/lib.sh` is bash-only (`BASH_SOURCE`); from zsh call `bash -c 'source …'`.
* Claude-harness background Bash tasks are killed at 10 min. For long Copilot runs launch
  detached — `(cmd > log 2>&1 &)` — and watch the log with a Monitor.
* Gradle task names matching `-t`/`--test` (including `readers=…,ext-test`) trigger the
  `createTestReports` finalizer; it fails if `runtime/logs/test-results/` doesn't exist
  (pre-create it) — the underlying load/test still succeeded.
* Compile with `-x checkstyleMain -x checkstyleTest` (checkstyle has maxErrors=0 and
  generated/test code won't pass it). `ofbiz --test` itself never runs checkstyle.
* Enforced-blindness recipe (verified): `--excluded-tools shell web-fetch` + cwd=sandbox
  (+ default path verification). `--deny-tool` alone only suppresses prompts.
* JUnit-3 idiom: constructor `super(name)`, `public void testXxx()`; suites roll back data
  once per SUITE (shared delegator), so fixtures must be namespace-unique (TaxFixture does
  this) and fixed-ID records (e.g. `_NA_/_NA_` TaxAuthority) need find-before-create.
* `mutations.py` requires each target string to occur EXACTLY once in
  `TaxAuthorityServices.java`; after any source change run the occurrence check before E2
  (see RUN-REPORT / `mutations.py apply` fails fast with a count).
* OFBiz quirk worth knowing: `getTaxAdjustments` line ~499 compares `itemQuantity !=
  BigDecimal.ZERO` by REFERENCE — bug-compatible behavior is the requirement; don't "fix" it.

## 5. The methodological invariants (do not relax)

1. **Copilot+Opus executes; Claude authors.** Pipeline stages, blind testgen, and the E3
   refactor must run under `copilot -p` (that's the deliverable being proven).
2. **Blind means blind.** The E1 agent gets `ir/` + `harness-contract.md`, nothing else.
   Repair rounds may feed back COMPILE errors only — never test failures/expected-vs-actual
   (that would leak behavior into "blind" tests).
3. **Red blind test ⇒ fix the IR (or harness), never the test.** After IR repair, rerun
   E1 with a FRESH agent. This loop ran once: 3 ambiguous claims clarified
   (VAT trigger = product's tax-inclusive catalog price, not store VAT-display; containment
   requires an address that itself passes the address guard) + 1 harness FK-order bug.
4. **harness-contract.md documents mechanics only** — fixture verbs and wiring, no behavior
   semantics; behavior lives exclusively in claims.
5. Validators are deterministic gates; the repair loop feeds their output back verbatim.

## 6. Open threads / next work (in rough priority order)

1. **Goal 2 — PR-history distillation** (`spec/drafts/Arlift-PR-history-distillation.md`,
   sequencing rationale in `spec/planning/goals.md`): build the Ledger (deterministic index:
   commits→PRs→tickets), then claim-driven mining backwards from `@Claim`/fragment bindings.
   Consumes the now-frozen claim schema as a fixed contract. The `provenance/*.yaml` files
   emitted by stage 5 are the stub to grow from (git-only today; no PR/ticket join yet).
2. **Goal 3 — architecture distillation**: `out/ofbiz-tax/ir/architecture/` (3 ADRs +
   3 mustache patterns) is a deliberate stub. Full goal: pattern library extraction as its
   own pipeline with the swap-test ("architecture can change, claims survive") as its
   validation — E3 was a first taste.
3. **Second target** to stress generality (spec suggests Fineract, or OFBiz
   `InvoiceServices.createInvoiceForOrder` as the "final boss"). Cost: one `targets/<name>/`
   plugin (descriptor + seam brief + harness + mutations); pipeline unchanged.
4. **Claim-status upgrade pass**: claims now carry `status: extracted`; the manifest knows
   pinned (backfill) vs blind-verified. A small deterministic tool could promote statuses
   (extracted → pinned → verified) from test reports — the "anti-drift spine" seed.
5. **Airlift repo is uncommitted** (user's repo — commit only when asked). The OFBiz-side
   work IS committed on that clone's local `airlift` branch (4 commits, `git log` there).
6. Minor debt: `run-pipeline.sh` repair rounds were exercised manually for the distill stage
   (interrupted run) rather than through its loop; `stage4` lets the agent run gradle itself
   (worked fine, but an external-runner variant like E1's would be more deterministic).

## 7. Reproducing the proof from scratch (if the world resets)

1. OFBiz: init wrapper, build, load seed tiers (§3 one-time setup).
2. `targets/ofbiz-tax/harness/install.sh` → run `airliftsmoke` (must be 2/2 green).
3. `./pipeline/run-pipeline.sh targets/ofbiz-tax all` (each stage validates itself).
4. `exit/run-e1-blind.sh` → expect green (IR ambiguity discovered here is the METHOD:
   repair claims, rerun fresh).
5. `exit/run-e2-mutations.sh`, `exit/run-e4-flip.sh`, `exit/run-e3-refactor.sh`.
