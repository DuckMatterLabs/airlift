# Airlift Stage 1 — Map: fragment inventory and classification

You are the Map stage of the Airlift distillation pipeline. Airlift distills business
behavior out of entangled legacy code into a typed, testable IR. This stage builds the
fragment map: the inventory of source fragments and their classification.

## Target seam

{{SEAM_BRIEF}}

## Task

Starting from the seam entry points above, trace every code path they execute — across
languages and layers (host-language source, external contract definitions, data-schema
definitions, configuration). Inventory every file that carries part of the seam's behavior.
Then produce a **fragment map**: partition the relevant source into fragments, give each a
stable ID, and classify each fragment. A fragment is a coherent region — a method, a block
within a method, a query construction, a condition cluster, a contract definition, a schema
definition, a configuration property group.

Classification per fragment (`role`):
* `business-logic` — encodes a business rule (something a domain expert would recognize
  and care about, independent of this implementation).
* `plumbing` — architecture: persistence queries, framework mechanics, dispatch, logging,
  transaction handling.
* `boilerplate` — license headers, imports, trivial null-defaulting, constants wiring.
* `fused` — business rule AND plumbing in the same expressions, inseparable without rewrite.
  These seams are the hard targets: for each, add `fused_note` naming exactly which business
  rule is entangled with which mechanism (e.g. "a threshold rule is encoded as a
  query-level condition inside the persistence lookup").

## Rules

1. **Total coverage of the primary source file(s)** listed in the seam brief: every line
   belongs to exactly one fragment. No gaps, no overlaps. (License header + imports may be
   one `boilerplate` fragment.)
2. Fragment IDs: `F-<ABBR>-NNN`, numbered top-to-bottom within each file. Choose a short
   uppercase abbreviation per file and record the mapping in a `file_abbreviations` table.
3. Each fragment: `id`, `file`, `symbol` (method/element name; plus a short `block` label for
   sub-method fragments), `lines: [start, end]`, `role`, `summary` (one factual line),
   `fused_note` (only when role=fused).
4. Do not paraphrase business rules yet — `summary` states what the fragment does, not why.
5. Where control flow jumps across languages or layers (source ↔ contract ↔ schema ↔ config),
   record the seam in a `seams` section: which fragments form one business transaction
   together.
6. Secondary files (contracts, schemas, config) need fragment entries only for the parts the
   seam actually touches — not total coverage.

## Output

Write exactly one file: `{{OUT_DIR}}/fragment-map.yaml`

```yaml
schema_version: "{{SCHEMA_VERSION}}"   # required top-level key, exactly this value
source_binding:
  repo: <repo directory name>
  commit: <run `git rev-parse HEAD` in the repo>
file_abbreviations:
  - abbr: <ABBR>
    file: <path relative to repo root>
inventory:            # every file you consulted
  - path: ...
    why: ...
fragments:
  - id: F-<ABBR>-001
    file: <path>
    symbol: <method or element>
    block: <optional short label for sub-method fragments>
    lines: [a, b]
    role: business-logic | plumbing | boilerplate | fused
    summary: one line
    fused_note: only when role=fused
seams:
  - name: ...
    fragments: [ids]
    note: how the business transaction spans them
```

Verify before finishing: the union of `lines` ranges over each primary file's fragments
covers the entire file with no overlap; YAML parses. Do not write any other file. Do not
modify source.
