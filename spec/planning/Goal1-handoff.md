---
confidence: proven
related: [spec/planning/MILESTONES.md, spec/planning/Airlift-m3-second-target.md, tests/runs/m2-freeze-report.md, spec/adr/README.md]
sources: [tests/out/ofbiz-tax/RUN-REPORT.md, tests/runs/m2-freeze-report.md, spec/log.md]
trajectory: >
  The operational bible for whoever continues the work (as of 2026-07-19:
  Copilot+Opus sessions, both author and executor roles — AADR-007).
  Sections 1-7 are the M1 record; section 8 is the M2 addendum and the
  takeover protocol. Superseded per-goal by future Goal<N>-handoff docs.
---

# Airlift — Goal 1 handoff

Status date: **2026-07-19** (M2 addendum: section 8). Audience: a future agent session
(or human) continuing this work with no memory of how it was built — as of M2, that
means **Copilot CLI + Opus 4.8 sessions in both roles** (authoring Airlift itself AND
executing pipeline runs; see §8.1). (Convention: each completed goal produces a
`Goal<N>-handoff.md` in `spec/planning/`; this is the first.)

## 1. Where things stand

**Goal 1 is COMPLETE** (per `spec/planning/Airlift-goal.md` + `spec/planning/goals.md`): the target-agnostic
distillation pipeline and Airlift IR exist, and were proven end-to-end on the required
execution harness — **GitHub Copilot CLI (`copilot -p`) with Anthropic Opus 4.8 via BYOK**
— against OFBiz `TaxAuthorityServices.rateProductTaxCalc` (sales-tax / tax-exemption seam).

All four exit criteria passed (details + evidence pointers in `tests/out/ofbiz-tax/RUN-REPORT.md`):

| Exit | Verdict |
|---|---|
| E1 blind IR-only test generation, green vs real code | PASSED — 35/35 (sandboxed agent: no shell, no web, no repo file access — empirically verified) |
| E2 planted bugs caught by blind suite | PASSED — 7/7, correct claims named each time |
| E3 behavior-preserving refactor stays green | PASSED — 675-line diff, 10 extracted methods, 35/35 green |
| E4 flipped tax-exemption rule caught AND named | PASSED — spine named the four TAX.EXEMPT claims |

The one mid-run course correction that shapes everything: **the pipeline and IR are
target-agnostic by user directive.** OFBiz is only the first target plugin. Never put
target-specific content in `pipeline/prompts/` or `ir-spec/` — it goes in `tests/targets/<name>/`.

## 2. Repo map (what each component is)

See `README.md` for the component descriptions. Quick index:

* `ir-spec/` — **frozen contract as of M2**: `IR-SCHEMA.md` v1.0 (claim atoms, constrained
  Gherkin, glossary, traceability, spine, attestations, architecture/provenance stubs;
  change process in its §11), `VERSION` (the supported `major.minor`, single source of
  truth), `CHANGELOG.md` (every schema change lands here — never live renegotiation).
* `pipeline/` — generic engine: `prompts/*.md` (5 stages + blind-testgen + refactor, all
  `{{PLACEHOLDER}}`-parameterized), `lib.sh` (Copilot BYOK wrapper `run_copilot`),
  `render.py`, `validate.py` (per-stage deterministic validators, version-checked),
  `run-pipeline.sh` (render → run → validate, ≤3 attempts with repair prompts; stage 4
  runs through the external runner — §8.3), `promote.py` (deterministic claim-status
  promotion — §8.2), `junitxml.py` + `stamp-backfill.py` (helpers), `test_*.py`
  (29 unit tests — keep green: `./.venv/bin/python -m unittest discover -s pipeline`).
* `tests/targets/ofbiz-tax/` — target plugin: `target.env` (all paths/commands), `seam.md`
  (seam brief injected into prompts), `harness/` (fixture DSL `TaxFixture`, base case,
  `@AirliftClaim` annotation, `TaxCalcResult`, smoke tests, testdef suites, `install.sh`,
  `run-tests.sh`, `report-claims.py` = the spine reporter, `harness-contract.md` = the
  mechanics-only doc agents write tests against), `mutations/mutations.py` (7 planted bugs).
* `tests/out/ofbiz-tax/` — pipeline products: `fragment-map.yaml`, `coverage-gaps.yaml`,
  `behavior-catalog.yaml`, `backfill-report.yaml`, `ir/` (34 claims, glossary 75 terms,
  domain-model, traceability, provenance/, architecture/), `e3/` (refactor artifacts),
  `RUN-REPORT.md` (the full narrative), `testgen-summary` lives in the blind sandbox copy.
* `tests/exit/run-e1-blind.sh … run-e4-flip.sh` — the four proof runners.
* `tests/runs/` — logs, rendered prompts, spine reports (`e1-spine.txt`, `e2-summary.txt`,
  `e2-*-spine.txt`, `e4-spine.txt`), blind sandbox (`tests/runs/blind-sandbox/`).
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
./pipeline/run-pipeline.sh tests/targets/ofbiz-tax all

# Exit proofs (E1 must run first — it generates + installs the blind suite)
./tests/exit/run-e1-blind.sh
./tests/exit/run-e2-mutations.sh
./tests/exit/run-e4-flip.sh
./tests/exit/run-e3-refactor.sh

# Harness alone
./tests/targets/ofbiz-tax/harness/install.sh          # idempotent copy into OFBiz
./tests/targets/ofbiz-tax/harness/run-tests.sh airliftsmoke|airliftbackfill|airliftblind
./.venv/bin/python tests/targets/ofbiz-tax/harness/report-claims.py \
    --results ../ofbiz-framework/runtime/logs/test-results/airliftblind.xml \
    --tests-dir ../ofbiz-framework/applications/accounting/src/test/java/org/apache/ofbiz/accounting/tax/test/blind \
    --ir tests/out/ofbiz-tax/ir
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
2. **Goal 3 — architecture distillation**: `tests/out/ofbiz-tax/ir/architecture/` (3 ADRs +
   3 mustache patterns) is a deliberate stub. Full goal: pattern library extraction as its
   own pipeline with the swap-test ("architecture can change, claims survive") as its
   validation — E3 was a first taste.
3. **Second target** to stress generality (spec suggests Fineract, or OFBiz
   `InvoiceServices.createInvoiceForOrder` as the "final boss"). Cost: one `tests/targets/<name>/`
   plugin (descriptor + seam brief + harness + mutations); pipeline unchanged.
4. ~~Claim-status upgrade pass~~ **DONE at M2**: `pipeline/promote.py` (§8.2); the
   ofbiz-tax IR now carries 27 `verified` / 3 `pinned` / 4 `extracted` with attestations.
5. ~~Airlift repo is uncommitted~~ **DONE**: committed and pushed
   (`git@github.com:DuckMatterLabs/airlift.git`); M2 landed as six per-workstream commits.
   Rule stands: commit only when the user asks.
6. ~~Stage-4 / repair-loop debt~~ **DONE at M2**: stage 4 runs through the external runner;
   the distill repair loop was demonstrated end-to-end (evidence:
   `tests/runs/m2-freeze-report.md`).

## 7. Reproducing the proof from scratch (if the world resets)

1. OFBiz: init wrapper, build, load seed tiers (§3 one-time setup).
2. `tests/targets/ofbiz-tax/harness/install.sh` → run `airliftsmoke` (must be 2/2 green).
3. `./pipeline/run-pipeline.sh tests/targets/ofbiz-tax all` (each stage validates itself).
4. `tests/exit/run-e1-blind.sh` → expect green (IR ambiguity discovered here is the METHOD:
   repair claims, rerun fresh).
5. `tests/exit/run-e2-mutations.sh`, `tests/exit/run-e4-flip.sh`, `tests/exit/run-e3-refactor.sh`.

## 8. M2 addendum (2026-07-19) — the contract is frozen; Copilot takes over

M2 is complete (all five exit criteria; evidence: `tests/runs/m2-freeze-report.md`;
session record: `spec/log.md` entry (e)). From here, **Copilot+Opus sessions carry both
roles** — authoring Airlift itself and executing pipeline runs. This section is the
takeover briefing: what changed at M2, and the discipline that keeps the project honest
when one model family sits on both sides.

### 8.1 Session protocol (non-negotiable, harness-agnostic)

The constitution lives in `CLAUDE.md` (named for the harness that authored M1/M2; its
content is harness-agnostic and binds every agent working on this repo). `AGENTS.md` at
the repo root points Copilot sessions at it. Every session:

1. **Orient**: read `CLAUDE.md` → `spec/index.md` → `spec/log.md` (newest entry) → this
   handoff → the spec of whatever milestone is next. Do not start work before this.
2. **Log**: append a `spec/log.md` entry (Done / Decisions / Open, newest first) before
   ending any session that changed anything. This is the only cross-session memory that
   survives a harness switch — treat it as sacred.
3. **Frontmatter** on every MD file you author or touch (template in `CLAUDE.md`).
4. **Commit only when the user asks.** The OFBiz clone's `airlift` branch stays clean.
5. The five standing invariants (§5 above, restated in `CLAUDE.md`) still bind. The old
   "Copilot executes; Claude authors" phrasing is now understood per AADR-007 as a ROLE
   split, not a vendor split: what must be preserved are the *guarantees* — fresh
   contexts for blind runs, enforced sandbox blindness, deterministic validators on the
   truth path, batch prompt-in/artifact-out execution. One vendor may hold both roles;
   the guarantees are enforced by process, never by trust.

Note: `run_copilot` passes `--no-custom-instructions`, so pipeline stage agents are
driven ONLY by the rendered prompts — `AGENTS.md` affects interactive development
sessions, never pipeline runs. Keep it that way.

### 8.2 The frozen schema and the promotion spine (new at M2)

* **Schema is v1.0** (`ir-spec/VERSION`). Changing it: minor = additive (new claim-kind,
  optional field, status value) with a `CHANGELOG.md` entry + VERSION bump + *extended*
  validators; major = breaking, requires a migration tool AND a fresh-agent E1 re-proof
  of the migrated ofbiz-tax IR. Never weaken a validator. A change touching a
  load-bearing decision updates `spec/adr/` at merge (register rules in
  `spec/adr/README.md`). Silent renegotiation is the thing M2 exists to prevent.
* **Every stage artifact declares `schema_version`** and every validator checks it
  against `ir-spec/VERSION` (major must match; minor ≤ supported).
* **Statuses move ONLY via `pipeline/promote.py`** — extraction stages always emit
  `status: extracted`; validators reject hand-written `pinned`/`verified` (they demand a
  supporting attestation). Promotion workflow after a green suite run:

  ```bash
  # 1. archive the JUnit XML with the run (stable evidence path — never point
  #    attestations at runtime/logs/, later runs overwrite it):
  mkdir -p tests/runs/<run-name>/ && cp "$TEST_RESULTS_DIR/<suite>.xml" tests/runs/<run-name>/
  # 2. emit the machine-readable spine (target-side binding extraction):
  ./.venv/bin/python tests/targets/<t>/harness/report-claims.py \
      --results tests/runs/<run-name>/<suite>.xml --tests-dir <suite-src-dir> \
      --spine-out tests/runs/<run-name>/<suite>.spine.yaml
  # 3. dry-run, ratify the printed diff, then apply (idempotent; rerun = no-op):
  ./.venv/bin/python pipeline/promote.py --ir tests/out/<t>/ir \
      --pinned-from   tests/runs/<run-name>/<backfill>.spine.yaml \
      --verified-from tests/runs/<run-name>/<blind>.spine.yaml   [--dry-run]
  ```

  Rules: backfill-green ⇒ at least `pinned`; blind-green ⇒ `verified`; any red bound
  test anywhere ⇒ no promotion for that claim (reported, exit 2); forward-only —
  demotion is M7's job, reading the same attestation records.
* **Line ranges are evidence at `source_binding.commit`** — the map validator reads the
  file via `git show` at that commit and there is deliberately NO working-tree fallback.
  If map validation fails after the source moved, the answer is never "re-extract line
  numbers against today's tree".

### 8.3 Pipeline changes (stage 4, drills, prompts)

* **Stage 4 external runner**: the agent only writes tests; `run-pipeline.sh` compiles,
  runs the suite, and stamps the `run:` block into `backfill-report.yaml`
  deterministically. Compile errors AND test failures feed repair rounds — sanctioned
  for this code-in-hand stage only; the blind path stays compile-errors-only forever
  (AADR-008). A target's `target.env` MUST define `TARGET_COMPILE_CMD`,
  `TARGET_BACKFILL_TEST_CMD`, `BACKFILL_RESULTS_FILE` or the stage refuses to start.
* **Repair-loop drill seam**: `AIRLIFT_SEED_SCHEMA_VERSION=0.9 ./pipeline/run-pipeline.sh
  <target> <stage>` seeds a version mismatch into the PROMPT only (validators still
  enforce the real version) — use it to prove the loop on a scratch target copy before
  trusting a new stage/target combination. Scratch pattern: copy the target dir, change
  `TARGET_NAME` (OUT_DIR derives from it), pre-seed the scratch OUT_DIR with upstream
  stage artifacts.
* All five stage prompts instruct emitting `schema_version` — if you add a stage, wire
  `{{SCHEMA_VERSION}}` through `render()` and add the validator check.

### 8.4 Gotchas earned at M2 (extends §4 — don't rediscover)

* **Tests ≠ claims.** 35 blind test methods bind 27 distinct claims of 34 total. When
  writing exit criteria or reports, count distinct claim IDs, never test methods (the M2
  spec's "35 verified claims" error cost a correction cycle).
* `ir: cannot validate: TypeError: string indices must be integers` from `validate.py`
  means a YAML *shape* error — most likely `claims:` emitted as a mapping instead of a
  list (the drill's attempt-1 organic failure). Known-opaque message; logged as minor
  validator UX debt in `spec/log.md` (e).
* **Suites run sequentially** — shared H2 database and gradle daemon; concurrent suite
  runs corrupt each other.
* `tests/runs/blind-sandbox/` and everything under `tests/runs/m2-*` are **frozen run
  evidence** — never regenerate, revalidate, or "clean up".
* Long runs (gradle, Copilot stages) — launch detached with a log file and poll the log;
  don't trust any harness's background-task patience.
* Promotion evidence must be reproducible: rerun the suites fresh before promoting
  (XML timestamps + `git rev-parse HEAD` in the target repo = the attestation's sha) —
  do not promote from stale XMLs whose tree state you can't attest.

### 8.5 M3 notes — real production code as the second target

The M3 spec (`spec/planning/Airlift-m3-second-target.md`) recommends Fineract; the user
has since directed that M3 runs against **real production code** — the user's target
choice supersedes the spec's recommendation (record the actual target in the spec when
known). What M3 must not lose regardless of target:

* **Cost of a new target = one plugin**: `tests/targets/<name>/` with `target.env` (ALL
  deterministic commands + the three backfill vars of §8.3), `seam.md` (seam brief),
  `harness/` (fixture DSL, mechanics-only `harness-contract.md`, install/run scripts, a
  spine reporter emitting `--spine-out`-style YAML — the binding mechanism is
  target-owned, AADR-011), `mutations/`. Use `tests/targets/ofbiz-tax/` as the template.
  **Pipeline and ir-spec stay untouched** — M3's exit criterion is that `git diff` over
  `pipeline/` and `ir-spec/` is empty or every delta is a justified versioned schema
  change.
* **M3 is also the measurement milestone** (reflexive C2 — the out-of-band fitness
  signals): friction log, harness cost accounting (R5), full-seam mutation sweep (R1),
  and **cross-model E1** (R2). The last one just got MORE important: with Copilot+Opus
  in both roles, extractor and blind generator share priors — same-model E1 green can
  hide ambiguity both sides fill identically. Generate at least one blind suite with a
  different model family where available; if cross-model E1 goes materially redder,
  the IR needs tightening the same-model proofs couldn't see.
* **Blindness recipe** (verified, §4): `--excluded-tools shell web-fetch` + cwd=sandbox;
  `--deny-tool` alone only suppresses prompts. E1 repair feedback: compile errors only.
* **Production-code cautions**: mutations and refactors run on a dedicated clone/branch
  (the clone's committed state is the revert baseline — mirror the OFBiz `airlift`
  branch pattern); never modify production source in place; know your fixture data's
  provenance before trusting green (the `loadAll` lesson generalizes — demo/live data
  bleeding into fixtures poisons every scenario silently); check the license/permission
  situation before committing extracted IR from proprietary code to any remote.
* Basis re-derivation probe (reflexive C3) is due ~M3: a fresh agent gets the problem
  statement and falsifier evidence — NOT the current schema — and re-derives an IR
  design; diff against v1.0. Budget it into the milestone.
