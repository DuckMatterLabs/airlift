---
confidence: proposed
related: [spec/planning/Airlift-goal-6-glossary.md, spec/planning/Airlift-goal-7-productionalization.md, Airbase, Maestro]
sources: []  # tier-3 original human prose
trajectory: >
  The founding vision; goals.md/MILESTONES.md project it into execution.
  Revised 2026-07-19 to fold in the Airlift/Airbase responsibility split
  (normative statement: spec/planning/Airlift-goal-7-productionalization.md).
  Human-authored - never silently regenerated.
---

# Airlift

(the name is a word play on Copilot aviation theme, plus a clever backronym: AI Resources for Leveraging Intelligence in Feature-development & Testing)
TL:DR
NOT a "better wiki". Not a wiki. This KB is a productized system of record with governance, ingestion, provenance, executable semantics, and strong human workflows. A good mental model is database-first semantic KB, behavior specs as executable meaning, AzDO as the action plane, and reports as the social proof loop. Optionally support Obsidian as the lens.
Airlift is the **fact half of a two-KB system**: it captures what the code provably does and how it got there. Its sibling **Airbase** captures what people intend. See "The Airlift/Airbase split" below.
The 3 original pillar ideas are:
* MD prose coupled with schema-rich frontmatter ties together agents, databases, and UI (see Airlift IR below for more details)
* Glossary as Lingua franca between AI and humans for lossless semantic capture
* AI manages AzDO tickets: AI managing code and AI managing AzDO tickets are two halves of one loop. AzDO tickets are the intent plane, the audit trail, and the velocity signal in one. (With the Airbase split, the intent plane is shared: Airbase owns intent capture; Airlift binds tickets to claims and provenance. The PR-lifetime action plane is Maestro — the org's Symphony-clone harness of agentic/human turns — with Airlift supplying grounding and gates inside those turns.)
  The KB is available to agents via Airlift-MCP.
  Will we automate ourselves out of a job? The honest answer: we automate the toil, not the judgment. Airlift and Copilot take the tactical execution; EMs and ICs move up to what doesn't automate — strategic direction, taste, and the calls that require knowing why. The job doesn't disappear. It moves up the value chain.
  We do not win by out-typing Copilot. We win by giving it better direction.
  Goals
  A live, authoritative KB-as-code for Engage teams that consolidates business logic, architecture decisions, glossary, concepts, processes, constraints, recipes, integrations, dependencies, and contracts in one place. The knowledge streams split by nature: code and commit history funnel into Airlift; meetings, schedules, PM and architecture specs funnel into Airbase — both through a rigorous shared glossary that disambiguates colloquial language. On the code side, the KB gradually inverts the source of truth — demoting the Java codebase to a secondary artifact — to unlock free refactoring, bold feature work, and truly agentic coding loops.

## The Airlift/Airbase split

Two KBs, two natures, one glossary between them. (Naming: **Team Brain** is the KB product; **Airbase** is our specialized per-team version of it — the names are synonyms in this document.)

* **Airlift is un-opinionated.** It is fact-driven from source code and commit history. It captures source-code **semantics** (claims, glossary terms, architecture patterns) and **trajectory** (provenance, the Ledger, drift history). It has no opinion about where the system should go — it testifies to what is and how it came to be.
* **Airbase captures intent.** It works with engineers — tribal knowledge, wikis, PM docs, ongoing meetings — distilling what people want and why. Direction lives there. The ingestion pipelines for meetings/specs/incidents and the three-tier human-intent discipline (transient / derived / human-authored) are Airbase's machinery.
* **The relationship:** Airlift is one of Airbase's **grounding sources** — when Airbase states intent about existing behavior, it cites Airlift claims (with tiers) instead of folklore. And Airlift subsequently **helps execute Airbase's vision of the future**: ratified Airbase intent becomes IR deltas, whose regenerated tests and Maestro-driven implementation turn vision into verified code. Facts flow up; ratified intent flows down; the diff between Airbase's intended behavior and Airlift's attested behavior is the backlog, made queryable.
* **The glossary is the shared span** (Goal 6): terms carry an anchor — code-anchored (Airlift, warranted by tests), intent-anchored (Airbase), or both. Both-anchored terms are where drift between what-code-does and what-people-mean becomes visible instead of latent.

## Why Airlift
### Why another KB?
  Comparing Airlift with adjacent systems:
* Team Brain (Sharepoint Wiki) - AI-powered tribal knowledge aggregator, atomizer, synthesizer. Self-evolving.
* Engage Team Brain instances - individual instances of Team Brain Wikis per Engage team (AI & Recommendations, Mobile, Notifications, etc). Collectively these are **Airbase** — the two names are synonyms: Team Brain is the KB product; Airbase is our specialized per-team version of it.
* Arch-insights - Architecture-oriented AI/human powered documentation aggregator and knowledge graph retriever
* Engage Product - PM (product) oriented human powered collection of MD prose, tools and skills. Non-evolving.
* Airbase - our per-team specialization of Team Brain (synonyms — see above): consolidates tribal knowledge, wikis, PM docs, and ongoing meetings; works with engineers, capturing and distilling intent. Airlift's designated sibling (see the split above).
* Airlift - AI powered codebase and PR history living knowledge cache and mirror. Self-evolving.
  Airlift complements Airbase and works in synergy with it: they capture intent with best-effort confidence; Airlift supplies the provable fact layer they ground on. What distinguishes Airlift from other KBs is that other KBs have best-effort confidence, as they must collect information from conversations or documents with limited provenance — that is not a flaw of theirs but a division of labor: intent has no test suite; code does.
###  Why compete with source code?
  Airlift specifically wants to be provable and eventually compete with source code as source-of-truth.
  But why compete with source code?
  The source code is ultimate snapshot of the system, but the system is more than just a code. The system has history, and it has future direction. Source code is just a snapshot. Here today, gone tomorrow. Replaced.
  By itself, source code does not contain enough information to capture intent, how did we get here, and where we are going. Source code is a position, not a trajectory. Agents writing new code using existing code for grounding is like photocopying a photocopy: the quality will be degrading.
  A skeptic immediately retorts: "well, that's why we ground AI with Architecture and PM documents".
  Two problems: the gap and the rot. Look at the leap being asked. The PM doc speaks intent ("users shouldn't be notified twice"); the code speaks mechanism. Between them: no shared vocabulary, no links, no intermediate structure. The agent bridges that gap by guessing -- fluently, plausibly, and differently on every run. Architecture and PM docs are also unverified snapshots -- and they drift worse than code, because nothing executes them and nothing fails when they go stale.
  Airlift gives agent the trajectory. It's a hologram, not a polaroid.
##  Obstacles
  Two hard problems sit at the center of this, mapping onto Phil Karlton's quip that "there are only two hard things in computer science: cache invalidation and naming things." Until we invert the source of truth, the KB is a cache over the code that can fall out of sync; the glossary is the naming problem.
  The real death spell isn't human inertia alone — it's spec drift. A KB that can be silently wrong is worse than none: the first time someone gets burned by stale info, they stop trusting it, and a distrusted KB is the inertia problem with a technical root cause. This is why CI-enforced drift detection is non-negotiable.
  Where this needs managerial support: when drift surfaces, the instinct is to flee back to the old ways. Sustaining the inversion will take the management muscle to insist we fix the drift rather than route around it — treating a stale KB claim as a build-breaking defect, not a documentation chore. That's the difference between a real source of truth and another wiki nobody trusts.
  Last but not least, this has a psychological problem.
>  Selling this is hard: engineers tend to reject conclusions they didn't reach themselves.

##  Requirements
  Driven by the goals, the requirements are:
* Initial extraction. Reverse-engineer business logic and architecture from the codebase into the KB, cutting through legacy artifacts, anti-patterns, and induced complexity rather than mirroring them.
* Canonical store. A versioned KB with a defined schema, stable IDs, and provenance on every object.
* Typed ontology, not prose. Plain MD is insufficient. The KB needs an explicit ontology of typed objects (business rule ≠ architecture decision; recipe ≠ incident workaround), their relationships, and lifecycle states. The glossary sits at the center — agentic coding depends on precise terms, not colloquial drift.
* Schema families + frontmatter. Frontmatter is required on every note. Every note belongs to a schema family with required/optional frontmatter fields; an explicit artifact taxonomy keeps the KB from becoming a dumping ground. In the manner of LLM-Wiki, the KB should be written to by AI only.
* Provenance & freshness (AI-maintained). Every object carries source, owner, confidence, effective date, supersedes, and review SLA. Without this the KB is persuasive but untrustworthy.
* KB-as-code. Every change is a reviewed PR with provenance (the meeting/commit/spec that produced the claim). Citations make wrongness auditable.
* Stable IDs as the reference layer. All references resolve to machine-resolvable stable IDs; friendly wikilinks are a display concern layered on top.
* Code sync. Track PRs closely so the KB stays in lockstep with the changing codebase.
* Anti-drift spine & verification — the CI-enforced check that KB claims still match code.
* AzDO as action plane. Beyond read access: work-item state transitions require or generate KB deltas, binding the work-management plane to the knowledge plane. (Shared with Airbase for intent; Maestro automates the PR-lifetime turns where Airlift's grounding and gates run.)
* Definition of Done. A DoD that both agents and humans understand and enforce across agentic and human flows
* Governance: Each Airlift instance is owned by a single Engage team. There are overlaps, but there is no single, shared Airlift. Airlift requires stewardship that is not achievable without clear accountability.
##  Proposed components
  I. Store — how knowledge persists
* The two-layer store: Markdown source of truth + schema-specific frontmatter → derived graph + vector index as a rebuildable materialized view (Kusto/graph DB/relational/JSONL/hybrid). Event sourcing: MD is the log, the graph is the projection.
* Verification spine (anti-drift): CI-enforced KB<—>code traceability — broken-link detection, knowledge-coverage metric, freshness/confidence decay. The enforcement arm of the store.
  II. Meaning — how knowledge stays precise
* Glossary / concept registry: typed first-class terms (canonical + aliases) — Ubiquitous Language, machine-enforceable. 
* Term linter: disambiguates incoming language, maps to canonical terms or opens a "new term?" ticket.
* Typed knowledge objects: decision records, executable specs (JBehave/Gherkin), business rules, recipes — each a queryable type, not loose files.
  III. Ingestion — how knowledge enters
* Airlift's own ingestion is code-side: source code, PR/commit history (the Ledger), incidents-as-linked-to-code. Meetings, specs, schedules, and architecture-doc ingestion (→ proposed KB diffs, human-ratified; perhaps Power Automate based) belong to **Airbase**; Airlift consumes its ratified outputs across the glossary bridge.
* Source-code symbol linker: KB entities bind to classes, APIs, configs, migrations, dashboards, tests as first-class references.
* AzDO MCP: work-item action plane; state transitions generate/require KB deltas (shared plane with Airbase; PR-lifetime automation via Maestro).
  IV. Serving — how knowledge is consumed
* Hybrid retrieval + agent interface: the MCP query plane for humans and agents — including Airbase (grounding citations) and Maestro's implementing/reviewing agents.
* Obsidian as lens (not store): navigation UI; wikilinks/tags generated from the derived graph, never hand-maintained.
* Daily + on-demand HTML reports: surfaced in Obsidian — point them at drift.
##  Provenance
  For KB to achieve high confidence status, it must be provable. The knowledge ingested into KB has to:
* come from multiple sources
* be cross-verified
* be provable against live source code via test suites
  
Multiple sources that are cross-verified:
* WHY: workitem tickets, ADRs, PR history — and, via Airbase, meetings and distilled engineer/PM intent
* WHAT: current codebase
* HOW: runbooks
* WHERE: project logistics
  
As a provenance demo, it should be able to generate test suites SOLELY from KB and have live code run against them, and vice versa, run new code generated from KB against existing tests.
##  Adoption
* Many AI-first processes die because they add capture overhead before they produce obvious daily value.
* To beat inertia, the KB must become the cheapest path for routine work: finding answers, generating reports, preparing meetings, opening work items, tracing a rule to code, and validating feature impact. Chat-over-KB MCP query interface — this is the carrot. Make the right thing the fast thing. On the day asking the KB beats asking a colleague or grepping Java, adoption stops being a fight.
* Daily and on-demand HTML reports inside Obsidian are a smart adoption wedge because they create immediate passive value even for skeptics.
  Management incentives
  Human resistance is not just laziness. It is rational behavior under bad incentives.
  If management rewards PR count, people will optimize the PR count. The KB must become visibly connected to delivery velocity.
  Suggested metrics:

| Metric	|Why it matters|
|--|--|
|Time to answer architecture question	|Shows knowledge retrieval value
|Onboarding time reduction	|Management-visible
|Incident diagnosis time	|Operational value
|PR rework caused by misunderstood behavior|	Quality value
|% work items with linked concepts/specs/tests	|SDLC adoption
|% behavior rules with executable tests	R|efactoring safety
|Code/KB drift count	|Governance
|Agent task success rate	|Agentic loop readiness
|Refactoring blocked by unknown behavior|	Shows remaining knowledge debt
|Repeated questions eliminated	|Knowledge reuse

## Indemnity problem
Consider this: AI will never replace lawyers/doctors because you can sue a lawyer/doctor for malpractice, but you cannot sue AI. Imagine leap of faith where say a PM is going to trust Airlift's answer (from KB) and NOT double check with a human. When a person answers the question, that person is responsible and potentially can be finger pointed, AI will not.
It's the deepest adoption blocker in the whole project — deeper than inertia, deeper than ego. Its name in the automation literature: the moral crumple zone (Madeleine Elish's term) — when a human-machine system fails, blame concentrates on the nearest human, like a car's crumple zone absorbing impact. The PM knows this instinctively. Asking a colleague isn't just information retrieval — it's a liability transaction. People don't ask colleagues for answers; they ask for absolution. The colleague's answer comes bundled with transferred responsibility; the KB's answer comes bundled with none. So even a 99%-accurate KB loses to a 90%-accurate human, because the human is blameable and the KB is not. Accuracy was never the currency. Cover was.
But notice: organizations already solved this problem, repeatedly, for other systems. Nobody double-checks payroll with a human. Nobody re-verifies git blame by asking who really wrote the line. Nobody got fired for citing the general ledger. Why? Because those are systems of record — organizationally sanctioned, so relying on them is itself the defensible act. "I checked the ledger" is due diligence, not recklessness. The trust isn't in the system's infallibility; it's in the fact that citing it transfers liability to the process.
That gives the design answer, and it's three moves we've mostly already planning:
1. Answers must be citable, not just correct. The KB never answers as an oracle ("the retention window is 30 days") — it answers as a clerk reading the record ("per ADR-0042, ratified by Priya, v3, effective March"). Now the PM isn't trusting "AI" at all; they're trusting a ratified human decision, retrieved by machine. The liability chain is intact — it runs to the ADR's owner, exactly as if they'd asked Priya, minus the interrupt. This is our provenance-on-every-object requirement revealed as the adoption mechanism, not just hygiene.
2. Accountability doesn't vanish — it relocates to the claim owner. Every claim in our schema already has an owner. Surface it in every answer. Asking the KB is still delegating to a person — the owner of record — mediated by retrieval. The finger-pointing target exists; it's just async.
3. Management must declare it the system of record — which is your mandate argument, now with teeth. "Grassroots plants it, ground rules make it stick" was culture-talk before; now it's a liability mechanism: only leadership can make "I relied on the KB" a defense rather than a confession. 

   
One honest scoping so we don't oversell: tier the answers. A ratified-ADR answer carries organizational backing; a derived-from-code answer carries "verified against source as of commit X"; an AI-inferred answer must say so and explicitly invite verification. 
Blur those tiers once — let an inference wear a ratified claim's confidence — and you've burned the very indemnity you built. And expect double-checking to persist early regardless; that's rational, not resistance. 
The goal isn't to eliminate verification on day one — it's to make trust auditable so the double-check rate decays with track record. Trust is earned transactionally,
> A KB nobody can blame is a KB nobody will trust. Airlift's answers must arrive with a paper trail — claim, owner, ratification — so that citing the KB is due diligence, not personal risk. We're not asking people to trust an AI; we're making the AI a clerk that reads them the signed record.

In future, after AI takes over some decision making, the claim owner will be AI. But that will happen after cuture already changed - and AI is accepted as the owner authority.

## References
[Digital Twin academic research article](https://arxiv.org/pdf/2503.07967)