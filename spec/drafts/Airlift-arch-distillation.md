# Airlift architecture distillation

Non-functional requirements or cross-cutting concerns are pieces of knowledge that is spread across source code, infra, YRFCs and even found in PM specs as constraints. We need to realize that architecture is NOT the system. Engage app is the system, but underlying architecture can be totally different (i.e. AWS based or written in TypeScript). But Airlift will not be adopted unless it can follow the existing architecture. If we view architecture as equally important but distinct part of Airlift, we can:
* ensure that generated code complies to architecture blueprint
* follows established patterns
* is able to evolve architecture without degrading biz behavior, and vice versa - validate if existing architecture permits evolving biz requirements, or constrains them
