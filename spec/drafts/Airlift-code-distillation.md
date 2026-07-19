# Airlift codebase distillation

> Prompt: In 1990s-2000s there was a lot of interest in visual programming, such as webmethods, BPMN, BPEL. It often included simply hooking up process via diagrams with included fine grain filters and logic gates in scripting language. I wonder if for the purposes of reverse engineering Java code into knowledge base, some kind of quasi executable documentation can be used, inspired by webmethods. If modern LLMs can reason over legacy spaghetti-like codebases, and quite often they use regular expressions to uncover dependencies, they should be able to reason much better over distilled business logic, specifically modularized, atomized for vector search, and free of grammar restrictions of human-oriented programming language. The problem to solve is how to "distill". Should we use AST parser, separate biz logic/behavior from architectural patterns for resiliency, error handling, performance, caching? Because the same biz logic can be applied via different architectures, and behavior should not marry implementation. What is my best behavior storage format? MD prose with embedded Java snippets? Some sort of webMethods or BPEL-inspired text that can be potentially executable? It's not just about capture, it's also about progressive disclosure so that LLM can load only the required behavior without relying on regular expressions, perhaps via vector search only.

## Discussion
BPEL failed because the moment diagram is executable, it must handle every edge case, with all incidental complexity - making diagram no longer serving the purpose of being simple.
Should separate biz logic from resiliency/error-handling/caching (aka cross-cutting concerns, AOP), because the same logic can wear different architectures - this is the right approach. Business behavior and platform behavior are orthogonal dimensions woven into the same text.
Your extraction pipeline is un-weaving them:
1. AST + static analysis for structure: call graphs, data flow, slicing. Mining tool: slicing the codebase into scoped units so the LLM classifies business-vs-platform-vs-plumbing over a bounded slice rather than raw files. Consumed, not stored. DO NOT store call graphs.
2. Drift sensor (ongoing): on each PR, regenerate the actual-state map and diff it against the claim bindings — "RULE-0107 claims to be implemented here; is the code that annotation binds still doing what the claim's tests demand?" The graph is the sensor reading, not the record.
3. LLM over slices for classification: is this line business rule, resilience pattern, or plumbing? This is judgment, not parsing — exactly what the LLM is for, and it works far better on an AST-scoped slice than on raw files.
4. Route by classification: business rules → behavior claims (Gherkin/decision tables); resilience/caching/error conventions → platform profiles (the shared claim-set from two turns ago — stated once, inherited by all services, never re-extracted per class); genuine plumbing → tagged incidental, verified droppable via the round-trip.
> Dijkstra, who coined separation of concerns, also gave you the mission statement: "The purpose of abstraction is not to be vague, but to create a new semantic level in which one can be absolutely precise." That's the KB in one sentence — a semantic level above Java where business behavior is stated precisely without marrying an implementation.
   

The typed claim stack (see Airlift IR) stands: Gherkin + decision tables for rules, statecharts (ASL-flavored) for processes, properties for invariants, schemas for the domain model, prose only for WHY. What your question genuinely adds is the orchestration notation choice — use the statechart/ASL lineage, which is BPEL's surviving DNA minus its sins.

Vector search? No. You're reaching for vector search as the escape from regex, but behavior knowledge has structure — call relations, state transitions, entity references, claim dependencies. Similarity search throws that structure away and hands back "vibes-adjacent chunks."

### Progressive disclosure: the real new problem — and the answer is graph-first, vector-second.
   The right retrieval is traversal along typed edges from stable IDs — which your architecture already has:
* Layer 0 (always loaded): glossary + domain map. Tiny, canonical terms.
* Layer 1 (index): capability catalog — one-line summaries with claim-IDs. The "table of contents" an agent scans.
* Layer 2 (on demand): full claim bodies, atomic, typed — fetched by ID, exactly your skeleton/transclusion chunks.
* Layer 3 (rarely): bound code via the @Claim("RULE-0107") annotations — the symbol linker as the final drill-down.
  This idea follows Karpathy's LLM Wiki pattern: LLM arrives to KB as amnesiac, the map is the bootstrap.

What is the durable KB<->code link, if not graphs? Answer: the `@Claim("RULE-0107")` annotations — and this is why they beat any external mapping. An external table ("RULE-0107 → FanoutService.java:142") would indeed be drift hell — every refactor invalidates line numbers and paths. The annotation is refactor-proof by construction: it lives in the code, so when the method moves, renames, or gets extracted, the binding travels with it automatically, carried by the same git commit. The only hand-maintained artifacts in the whole system are the claims themselves; the binding rides the code, and the graphs are regenerated weather.
## Implementation
Many of these are overkills, these are pre-AI era code analyzers. We do not need to store AST or LST. Our provenance (proof that AI extracted behavior correctly) is test generation loop.
> It is important to understand that Airlift is AI-first, non-deterministic by nature KB, but is has provenance mechanism, whereby making said mechanism a must-have

However, to point AI to the correct code, we need a GPS system (code index):
* https://github.com/github/codeql
* https://github.com/openrewrite/rewrite-all/tree/main
* https://github.com/joernio/joern
* https://github.com/scip-code/scip
##  Business Behavior Atom
  Canonical YAML
```YAML
  rules:
  - id: inactive-recipient
    priority: 100
    when:
      op: ne
      left:
        ref: recipient.status
      right:
        literal: ACTIVE
    then:
      eligible:
        literal: false
      rejection_reason:
        literal: RECIPIENT_INACTIVE

- id: sender-blocked
    priority: 90
    when:
      op: eq
      left:
        ref: relationship.sender_blocked
      right:
        literal: true
    then:
      eligible:
        literal: false
      rejection_reason:
        literal: SENDER_BLOCKED
```
is too verbose (token budget) and requires additional skill to describe schema.
On the other hand, Gherkin's syntax already known to LLM and is compact:
```
behavior: |
  Rule: inactive-recipient
    Given recipient.status != ACTIVE
    Then eligible = false
    And rejection_reason = RECIPIENT_INACTIVE
  Rule: sender-blocked
    Given relationship.sender_blocked
    Then eligible = false
    And rejection_reason = SENDER_BLOCKED
```
Gherkin inside YAML DSL envelope is the Business Behaviour Atom format that combines Gherkin structured language and a lightweight behavioral DSL.
Gherkin describes examples of behavior, not necessarily the behavior itself. It is prone to scenario explosion.
Therefore combining gherkin with YAML that compensates for deficiencies is recommended, for example in case of feature flags, prioritized decision tables. For example:
* Natural language is not truly formal - Require glossary terminology, apply constraint onto verbs: "Given Alice blocked Bob" define exactly what "blocked" means in YAML annotation.
* Avoid And chains that encode algorithms - refer to a generic algorithm as "And recipient is validated" where validated is a named algorithm.
* Gherkin is poor for relations - define rule relations in YAML envelope
  Examples where Gherkin needs additional muscle from YAML:
* Hundreds of reusable decisions
* Complex rule precedence
* Calculations
* State transitions (Mermaid?)
* Operational guarantees
* Provenance
* Relations and dependencies
* Flights/versioning
* Cross-repository implementation bindings
### Progressive-disclosure advantage
  Store each atom as a separate chunk in DB. The LLM receives only the top-k most relevant atoms for any question, dramatically reducing context size and noise.
  The canonical artifact can be rich while the LLM receives a reduced projection.
  First retrieval is economical:
```YAML
  id: notification.recipient.eligibility
  summary: Determines whether a candidate recipient may receive a notification.
  depends_on:
  - identity.recipient-status
  - social-graph.block-relationship
```
  Then, LLM is free to pull remaining data selectively (this is where progressive-disclosure MCP comes handy).
  Separate atoms: behavior expansion (recipient-status.yml, social-graph.yml)
  Separately: evidence expansion (backlinks to code, PR history)
###  Optimal representation
```text
  Thin YAML envelope
    +
  symbolic, constrained Gherkin behavior
    +
  compact expression invariants
    +
  externalized provenance and evidence (separate files)
```
  Code Digital Twin: A Knowledge Infrastructure for AI-Assisted Complex Software Development paper happens to be validating Airlift thesis, although it does not go as far as replacing code as source of truth.
  Cross-checking the article with proposed approach, the implementation is validated.
```text
  Paper’s model                         Airlift implementation
  ────────────────────────────────────────────────────────────────
  Knowledge graph / typed code map  →  YAML metadata + graph index
  Concise functionality card        →  Constrained Gherkin behavior body
  Rationale spine                   →  Separate rationale/constraint fields
  Evidence store                    →  Code, test, PR, issue references
  Graph-first Twin-RAG              →  Retrieve cards through typed relations
  Token-bounded context package     →  Materialize only required Gherkin rules
```  
##  How PR history fits
  PR history fits as separate evidence files for behavior claims and additional provenance - as we prove that the behavior was specifically developed over time, confirmed by a decision, and not just a hallucination from the current state of source code. Note that this also answers the citation concern, see indemnity problem.
```YAML
  history:
  introduced:
    feature: ENGAGE-12842
    pull_request: notification-controller#918
    commit: 94ade21

changes:
    - version: 2
      feature: ENGAGE-13281
      semantic_change:
        - blocked-sender rule now overrides mention preference

- version: 3
      pull_request: audience-service#412
      implementation_change:
        - moved evaluation into audience-service
      behavior_changed: false
```
## TBD: Architectural Aspects
See Airlift - arch distillation.

## Index vs graph db vs vector db
BOM (bill-of-materials) is inverted index. Since the vocabulary is controlled, we don’t need fuzzy matching; an exact lookup works. When an LLM needs context, it can be given a list of available canonical terms and choose the relevant ones, or we can pre-parse the user’s intent into canonical terms using the same controlled glossary.
Pros of graphDB: The BOM lists direct memberships. But what if you want to find “all atoms that are transitively affected by changing a particular API endpoint”? The BOM would require you to predefine a depends_on index. Another example - Multi-hop rationale chains. A constraint may have been introduced because of a prior decision in a different domain. The BOM can’t easily express “rationale A was overridden by rationale B in 2023”. A graph can model these versioned links directly and retrieve the whole chain.
Pros of vector search: what if we want to find adjacent concepts? The LLM may want to retrieve “related constraints” but can only name exact canonical terms it already knows about. Another application is serving human queries that may use proper keywords.
In other words, vector search and graph DBs become useful when the retrieval is speculative, associative, or requires transitive closure.
Verdict: start with index, add graph db, vector search as needed.
