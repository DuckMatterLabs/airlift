# Airlift IR

## Problem statement
To achieve high-confidence, provable knowledge base. I am thinking of ingesting from multiple sources:
* WHY: workitem tickets, ADRs, PR history,
* WHAT: current codebase,
* HOW: runbooks,
* WHERE: project logistics.
  To have provable KB, I want to be able to generate test suites SOLELY from KB and have live code run against them, and vice versa, run new code generated from KB against existing tests. But ingest into what? Some kind of pseudo-code? Or MD prose that I can feed input JSON, have agent massage it according to the rules, and produce output artifacts that must comply with existing tests.
  What is the KB internal format that I can perform behavior-lossless transpilation from Java, while dropping all boilerplate artifacts?

## Discussion
The KB is sufficient iff code regenerated from it passes the behavioral test suite, and tests generated from it pass against live code. Losslessness isn't a property of the format — it's an operational property proven by the round-trip. That's bisimulation: two systems equivalent with respect to observable behavior. You don't need to argue your IR captured everything; you demonstrate it, per claim, in CI. That disqualifies both candidates:
* Pseudo-code — worst of both worlds: unexecutable (can't verify) yet code-shaped (re-imports incidental structure). It's Java with the types stripped off — lossy in exactly the undisciplined way you fear.
* MD prose + "agent massages it" — puts a generative step on the truth path. Prose→test via LLM is nondeterministic; the same claim yields different tests on different days. You outlawed this pattern yourself: never a hallucination on the critical path.
  The answer is that there is no single format. The IR is a small stack of typed claim-kinds, each with a deterministic compiler to tests.
  The stack, mapped to the ingestion sources:

| Knowledge kind	| IR format                                                                                                                  | 	Compiles to	Source|
|--|----------------------------------------------------------------------------------------------------------------------------|--
| Behavior rules| 	Gherkin/JBehave scenarios + decision tables                                                                               | 	Acceptance tests (both directions)	| code, tickets
| Domain model	| Typed schema (CUE / JSON Schema: entities, fields, constraints, units)	                                                    | Validation + serialization round-trip tests	| code
| Invariants	| Property statements ("fanout is idempotent per (user, notification)")	Property-based tests (jqwik)	                        | code, ADRs, incidents
| Processes/lifecycles	| State machines (statecharts: states, transitions, guards)	Model-based tests — illegal transitions must be unrepresentable	 |code, runbooks
| Contracts	OpenAPI / AsyncAPI / IDL	| Contract tests	                                                                                                            | code, integration docs
| WHY (rationale, tradeoffs)	| MD prose + frontmatter, linking to the claims above	Nothing — and that's correct	                                          | ADRs, meetings, PRs
| Glossary	| Typed term entities binding every name above	Term-linter checks	                                                           | all
The round trip looks like:
```YAML
Java ──extract──▶ typed claims ──compile──▶ generated tests ──run against──▶ live code
                      │                                            │
                      │                             mismatch = extraction error
                      │                                OR dead/buggy code (gold!)
                      ▼
              regenerated code ──run against──▶ existing test suite
```
Mismatches in direction 1 are the quality gate on extraction — and occasionally a genuine find: the KB says X, the code does Y, and the code was wrong all along.
Direction 2 is the disposability proof: the day regenerated code passes the legacy suite, you've demonstrated the KB is the master and Java is a projection.
### Boilerplate
"Boilerplate" is a runtime architecture, and some of it is load-bearing. Retry policies, transaction boundaries, timeout values, null-handling conventions look like boilerplate but carry behavior — drop them silently and the regenerated code passes functional tests while failing in production. The discipline: extraction never discards, it classifies. Everything is either (a) promoted to a claim ("3 retries, exponential backoff, jitter"), (b) demoted to a platform profile — a shared claim-set covering conventions all services inherit, so it's stated once, not per-service — or (c) explicitly tagged incidental, verified droppable. The platform profile is the trick that keeps the KB clean without the silent-drop risk: boilerplate isn't deleted, it's deduplicated into a claim.
### Coverage gaps
The test suite is the measure of losslessness — so coverage gaps are knowledge gaps. Behavior no test observes is behavior the round-trip can't protect. That's not a flaw; it's the honest boundary, and it gives you the metric your verification spine wanted anyway: claim coverage — what fraction of live behavior is pinned by a KB-derived test. Below the line, regeneration is unsafe; the number tells you exactly where.
### Annotated Java code as KB
What if we have existing Java code, augmented with semantic comments, annotations, gradually restructured by AI to serve as both current live code as well as living spec for evolution?
Objections:
* Every refactoring becomes self-surgery. Self-surgery is precisely the loss of stable ground truth. In the two-artifact world, refactoring is safe because the KB holds still while code moves — the tests derived from it are the fixed operating table. When the patient is the table, every refactor is simultaneously a spec change, and you can never distinguish "I restructured the expression of the rule" from "I changed the rule." That distinction is the whole game.
* Conway lock-in is structural, not incidental: knowledge embedded in code inherits code's decomposition. A business rule that today spans three services (because Conway) stays sharded across three annotated files forever — the spec can never state it as the one rule it actually is. Worse, some knowledge has no code location at all: cross-service invariants, WHY-rationale, business intent. Single-artifact gives them nowhere to live. The attraction of the approach — "no parallel structure" — is exactly its cage: the spec can never be shaped better than the code it lives in.
* Literate programming (Knuth) and doc-as-code, and forty years of that history teaches one lesson — comments and annotations drift because nothing executes them. The semantic comments would be unverified prose sitting next to verified code; the compiler enforces the code half and ignores the spec half. You'd have rebuilt the stale-Javadoc problem with better intentions.
  But — the proposal contains the right migration mechanism, wrongly promoted. Annotations shouldn't be the KB; they should be the binding layer to it. Annotate Java with claim-IDs:
```Java
 @Claim("RULE-0107")  // dedup window — canonical claim lives in KB
  public boolean shouldSuppress(Notification n) { ... }
```  
Code carries pointers to knowledge; the KB carries the knowledge, shaped by the domain rather than by Conway; the round-trip stays provable because there are still two artifacts to diff. You get the pragmatism of "start from the Java we have" as the on-ramp, while keeping the destination the inversion — instead of paving the on-ramp and calling it the destination.

##  Classifications of knowledge streams
Streams are qualified by truth-status. Three tiers, and each has a different home in architecture:
1. Transient input (discard after extraction). Meeting transcripts. They're watered-down — a transcript is signal buried in hours of noise. It is never source of truth. Its job is to be mined: extract the ADR, the progress delta, the planning artifact — those typed objects are the canonical output, gated by human approval before commit (your instinct, correct — a meeting decision isn't real until a human ratifies the extraction). Then the raw transcript is archival sludge, not KB. This is the chunk-and-destroy case.
2. Derived-from-code (regenerable, and that's the point). The bulk. Here's the move that resolves your whole earlier anguish: for code-derived prose, the source of truth is the code, not the prose. So regenerating it isn't drift — regeneration is how it stays true. The thing feared (AI rewriting the narrative) is correct behavior when the narrative's ground truth is the codebase, because you can always re-derive and diff against the actual source. Our drift check writes itself: re-extract from current code, diff against the stored prose, flag deltas. Code-derived notes are safe to regenerate precisely because they have an external ground truth to be checked against — the exact property the authored skeleton lacked. Watch the circularity in tier 2. If AI reverse-engineers prose from code, and agents then code from that prose, you've built a loop that can drift from reality while staying perfectly self-consistent. The escape is that tier 1 and tier 3 are the only injection points of human intent — ADRs from meetings, PM requirements. They're the ground truth that keeps the code <-> prose loop honest. Which means the rare human content isn't a minor 5% — it's the anchor the other 95% is checked against. Small in volume, decisive in weight.
3. Genuinely human-authored (rare, precious, protected). PM requirements, and the intent behind decisions — the "we chose X because of a business reason invisible in the code." This is the small set where your earlier preservation argument fully holds. It has no external ground truth (the why isn't in the code, isn't fully in the transcript), so it must be authored, versioned, and never silently regenerated. It's rare — which is exactly why it's affordable to protect rigorously. Human narrative prose should preserved intact but should be processed into narrative skeleton + transclusions from extracted atomic (zettelkasten) concepts. The atoms versioning becomes the core mechanism for detecting drift in human-authored prose.
   The principle that ties it together:
   Regenerate what has an external ground truth; protect what doesn't. Code-derived prose is a view (re-derive freely); human intent is a source (guard it). Most Obsidian prose is AI-generated from code; transcripts are mined-then-discarded; human prose is the rare exception.
   Glossary frontmatter sample
```YAML
   id: concept.notification-recipient
   canonical: Notification Recipient
   aliases:
  - receiver
  - audience member
  - target user
   forbidden_or_ambiguous:
  - user
  - member
   definition: >
  A user or entity selected to receive a notification after audience resolution,
  filtering, permission checks, and preference suppression.
   not:
  - The author of the event
  - The raw audience before suppression
  - The device endpoint
   related:
  - concept.audience-resolution
  - concept.preference-suppression
  - contract.notification-send-request
   status: canonical
   owner: notifications-platform
```

## Knowledge substrate
The KB should have three simultaneous forms:

| Layer	| Purpose	| Storage candidate |
|---|---|---|
| Markdown / LLM Wiki	| Human-readable canonical pages, local editing, Git review, Obsidian navigation	| Git repo|
| Typed knowledge graph |	Entities, relationships, ownership, dependencies, contracts, provenance	| Kusto tables, graph DB, relational DB, JSONL, or hybrid|
Search/index layer|	Retrieval for agents and humans |	Vector index + lexical index + code symbol index|

The key is not choosing one format. The key is making every canonical fact addressable:
```YAML
concept: notification-recipient-resolution
owner: Engage Notifications
aliases:
  - audience expansion
  - recipient fanout
  - targeting
artifact_type: business-concept
status: canonical
source_evidence:
  - code path
  - PM spec
  - incident
  - architecture decision
related:
  - notification-delivery-contract
  - audience-service
  - muted-user-rule
```