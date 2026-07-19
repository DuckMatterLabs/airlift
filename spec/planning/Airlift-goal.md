# Airlift spec

Create distillation pipeline use Github Copilot CLI as the harness (copilot -p), with Opus 4.8 as the model. Use Anthropic API key from [.env](../../.env) file.
You develop Airlift IR, you author this pipeline; but the delivered artifacts run under Copilot CLI with Opus 4.8.

## Goal summary

Develop the IR and use it to distill business logic from one package of the spaghetti test code (OFBiz TaxAuthorityServices, tax-exemption claim). 
Commit-history and architecture distillation are separate goals; you may stub them as needed to supply grounding context for valid test generation. 


## Details

Study these articles to understand Airlift:
2. [Airlift whitepaper](../drafts/Airlift-article.md)
3. [Airlift code distillation](../drafts/Airlift-code-distillation.md)
4. [Airlift PR history distillation](../drafts/Arlift-PR-history-distillation.md)
1. [Airlift architecture distillation](../drafts/Airlift-arch-distillation.md)
5. [Airlift-IR](../drafts/Airlift-IR.md)


You will use test generation based on IR that the original source code would have to pass as exit criteria.

Method. Distill in passes; the following decomposition works, and you may refine it before execution if you can justify the change in one line — but execute a fixed plan, don't improvise mid-run.
1. Map. Inventory source files and repos. Classify each fragment: architecture/plumbing, boilerplate, or business logic. Flag where they're fused rather than separated — those seams are the hard targets. → Output: a fragment map. 
2. Test landscape. Survey existing tests; mark which business behaviors they actually cover versus leave bare. → Output: a coverage gap list. 
3. Catalog. From the map and coverage, enumerate the distinct business behaviors present. → Output: a behavior catalog. 
4. Backfill tests. Add tests for uncovered behaviors, so the catalog is pinned by executable checks before you distill. (Note: these are code-in-hand tests — distinct from the exit criterion's blind, IR-only generation.)
5. Distill to IR. Emit the business-logic IR. Consult git history where a behavior's intent is unclear. Annotate source fragments with stable IDs so each IR claim traces back to code. → Output: the IR, plus the claim↔fragment ID map.

Prior art (optional). https://github.com/Egonex-AI/Understand-Anything (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/Understand-Anything) implements a comparable multi-stage, specialized-agent walker (project scanner, file analyzer, architecture analyzer). Consult it for concrete patterns only if useful — borrow ideas, don't inherit its structure. Airlift's decomposition is defined above; UA does not override it.

Prior art for deterministic AST parser (optional):
* https://github.com/adamtornhill/code-maat (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/code-maat)
* https://github.com/tsantalis/RefactoringMiner (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/RefactoringMiner)
* https://github.com/ataraxie/codeshovel (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/codeshovel)
* https://github.com/jodavimehran/code-tracker (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/code-tracker)
* https://github.com/giancarloerra/socraticode (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/socraticode). AST-aware code chunking.  It uses AST‑aware parsing plus hybrid search and dependency graphs

Extract architecture representation as a separate spec. 
The difference between biz logic distillation and architecture distillation is that architecture can be evolved or swapped without affecting biz functionality. 
Similarly, the biz functionality should be able to evolve within architectural constraints until it may hit a limit at which point architecture might have to evolve.
Proposed format for architecture IR is ADRs. But because our org uses repeatable patterns, you need to extract patterns into pattern library. 
Each pattern should be unambiguous spec using code snippets, config snippets, terraform snippets, python or shell scripts. 
Use mustache syntax to express parameters. Then, refer to patterns from ADRs possibly specifying concrete parameters.

# Spaghetty repos

Pick a package or packages from below repos to test distillation pipelines.

## OFBiz
https://github.com/apache/ofbiz-framework.git (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/ofbiz-framework)
OFBiz is a massive enterprise resource planning (ERP) system and is famously cited in academic research as a baseline for detecting "Spaghetti Code" and "Swiss Army Knife" anti-patterns.
The Entanglement: Business logic is frequently buried inside XML-based "minilanguage" scripts, FreeMarker UI templates, and direct database entity engine calls.
AI Challenge: The engine will have to trace control flow that jumps horizontally between Java, XML definitions, and UI templates to reconstruct a single business transaction.

Your first probe wants a rule that's crisp enough to state in one sentence yet tangled enough to hurt. 
That's TaxAuthorityServices.rateProductTaxCalc (772 lines) and its private workhorse getTaxAdjustments.
Why it's the ideal seam: a single tax calculation reaches into 12 entities (TaxAuthority, PartyTaxAuthInfo, TaxAuthorityRateProduct, ProductStore, PostalAddress, ProductCategoryMember…), 
resolves jurisdiction from a shipping address, and applies several business rules — exemption, per-jurisdiction shipping taxability, minimum-item-price thresholds — all inline, entangled with entity-engine queries (EntityQuery.use(delegator).from(...)) 
and wired to the outside world only through the XML service contract calcTax in services_tax.xml. Rule, persistence, and architectural plumbing are the same tissue. 
Exactly your target pathology.

Advanced probe: InvoiceServices.java — at 3,863 lines it's the monster method (createInvoiceForOrder), which is the right final boss but the wrong first bite.

## Fineract
https://github.com/apache/fineract (See cloned repo in /Volumes/Dancer/Develop/AIRLIFT/fineract)
Fineract is an open-source core banking platform. While it aims for modern architecture, its legacy iterations and massive scope make it a prime example of "pattern spaghetti."
The Entanglement: Extreme over-engineering. You will find massive class hierarchies, deep ORM nesting, and heavy use of abstractions where simple procedural logic would suffice.
AI Challenge: The engine must learn to see through layers of boilerplate and abstraction interfaces to identify the actual financial business rules hidden beneath.
It's almost purpose-built to be Airlift's adversary. It's simultaneously an ERP and a framework, which is exactly the "business logic married to architectural patterns" pathology you're describing. Concretely, the entanglement lives in:


# Exit criteria

Make sure the pipeline runs end-to-end on the actual target Copilot+Opus harness before you call the goal done.

Exit when:
* A subagent writes tests using only the Airlift IR spec — it never sees the real code. Those tests then run green against the real code. (Proves the spec captured the behavior faithfully, blind.)
* Plant a handful of small, deliberate bugs in the real code. The IR-generated tests catch most of them — they go red on the broken versions. (Proves the tests actually check something, instead of passing no matter what.)
* A subagent refactors the ugly code, preserving behavior. The tests stay green. (Proves a good cleanup survives.)
* Make one deliberately wrong change — like flipping the tax-exemption rule. The tests go red, and the spine says which claim was violated. (Proves a bad change gets caught and named.)

