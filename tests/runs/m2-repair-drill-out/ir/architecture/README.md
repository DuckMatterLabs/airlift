# Architecture stub — ofbiz sales-tax seam

This directory records where *architectural* knowledge lives so that the behavior claims can
stay pure domain vocabulary (no class/entity/service/config identifiers). It is deliberately a
stub: enough to orient a reader and to parameterize a test harness, not a full design doc.

Boundaries documented:

- `adr/0001-public-contract-boundary.md` — how the seam is wired to the outside world
  (service-engine contract). Pattern: `patterns/service-contract.mustache`.
- `adr/0002-persistence-pattern.md` — how business rules are fused with entity-engine query
  construction. Pattern: `patterns/entity-query-condition.mustache`.
- `adr/0003-integration-test-harness.md` — how the seam is exercised under test through the
  fixture DSL. Pattern: `patterns/harness-fixture.mustache`.

Claims reference behavior only; fragment IDs in `traceability.yaml` bind those behaviors to the
architectural sites described here.
