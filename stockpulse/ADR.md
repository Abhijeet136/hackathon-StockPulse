# StockPulse: architecture decision record

Format: Context → Options → Decision → Tradeoffs. Only decisions that are implemented in the current codebase are recorded here.

## ADR-1: Use a modular monolith instead of a service-oriented split

**Context.** The system is small enough to be owned by one developer, but the domain still needs clear boundaries for pricing, inventory, and suggestion orchestration.

**Options.**
- Microservices with separate pricing, inventory, and advisor services plus an event broker.
- A single Spring Boot application with explicit package boundaries and layered responsibilities.

**Decision.** Build a modular monolith. HTTP controllers handle only transport concerns. `SuggestionService` coordinates flows between domain logic, strategies, and persistence. Strategy implementations decide pricing and reorder actions. Repositories persist state. Domain entities retain simple lifecycle rules such as `recordSale`, `recomputeStatus`, and accept/reject transitions.

**Tradeoffs.** The codebase is more structured than a single script, but it remains a single deployable unit. This keeps the solution lightweight while preserving seams for future extraction if the system grows.

## ADR-2: Separate pricing and reorder decisions into distinct strategy interfaces

**Context.** Pricing and reorder quantity are independent decisions with different data and constraints.

**Options.**
- One combined advice endpoint that returns both outputs in a single LLM call.
- Two domain-specific strategy interfaces with independent execution paths.

**Decision.** Introduce separate interfaces: `PricingStrategy.suggest(PricingContext)` and `ReorderStrategy.suggest(ReorderContext)`. Context and result objects are plain records rather than JPA entities, which keeps strategies unit-testable without database setup. Each strategy family has its own fallback path, and strategy selection can be mixed at runtime.

**Tradeoffs.** This increases API call count and slightly raises latency. The benefit is stronger separation of concerns, easier testing, and more flexible runtime configuration.

## ADR-3: Support runtime strategy switching without redeploying the app

**Context.** The demo and operations workflow requires rapid AI-to-rules switching for pricing and reorder behavior without a restart.

**Options.**
- Spring Cloud refresh mechanisms and external config.
- A bean map plus an atomic runtime selection per strategy type.

**Decision.** Spring automatically collects all pricing and reorder strategy beans and exposes them through `StrategyRegistry`. The registry keeps an `AtomicReference<String>` for the active pricing strategy and active reorder strategy. `PUT /admin/strategy` updates the active name, and unknown values return `400`. The same registry is used by both HTTP endpoints and async listeners.

**Tradeoffs.** The active selection resets to the configured default after a restart. That tradeoff is acceptable for a demo and operational console where runtime flexibility matters more than long-lived state.

## ADR-4: Use Gemini as the primary LLM provider with explicit safety and fallback rules

**Context.** LLM calls can fail due to timeouts, quotas, malformed responses, or low-quality suggestions. The system must remain operational even when the AI provider is unavailable.

**Decision.**
- **Provider:** Google Gemini `gemini-3.5-flash-lite` via the native `generateContent` API using Spring `RestClient`.
- **Authentication:** the API key is read only from `GEMINI_API_KEY` and sent in the `x-goog-api-key` header; it is never logged.
- **Timeouts:** 3 s connect timeout and 15 s read timeout.
- **Parsing defenses:** strip markdown fences, extract the outermost JSON object, parse with Jackson while ignoring unknown fields, and tolerate enum casing differences.
- **Validation bounds:** price must be positive and within ±50% of the current price; confidence must be in the range 0–1; direction must align with the delta sign; reorder quantity must remain within a constrained range.
- **Failure model:** any LLM exception is caught, logged, and replaced by the rule-based fallback result. The HTTP layer never surfaces a provider error directly.
- **Transaction boundary:** the LLM call runs outside the transactional database write. Only the final suggestion save is wrapped in the short transaction used by `SuggestionWriter`.

**Tradeoffs.** This design intentionally prefers resilience over perfect AI completeness. A transient 429 or parsing issue yields a rules-based suggestion rather than a delayed or failed user flow. The tradeoff is that genuinely large price moves may be rejected by the validation window.

## ADR-5: Trigger suggestions via domain events to keep the sale path fast and signal-driven

**Context.** Suggestions should be created on meaningful business signals without blocking the main order request or generating duplicate recommendations.

**Options.**
- Polling-based scanning jobs.
- Event-driven suggestion generation from domain changes.

**Decision.** The order flow updates stock and velocity synchronously and publishes domain events when stock crosses a threshold or demand spikes beyond the configured multiplier. Two asynchronous listeners invoke the same shared suggestion-generation path. Both listeners run on an `advisorExecutor` thread pool and use `AFTER_COMMIT` semantics so they react only after the originating transaction is complete.

**Deduplication.** Suggestions are deduplicated per product, trigger reason, and suggestion type. The normal guard is `existsBy...PENDING`. A unique dedupe key provides a race-condition safety net when two signals arrive at the same time. On duplicate detection, the system simply skips the second suggestion without failing the main flow.

**Tradeoffs.** In-memory event delivery is intentionally lightweight and does not provide a durable outbox. This is acceptable for advisory suggestions but would need an outbox or message queue for stronger crash recovery guarantees.

## ADR-6: Keep the solution extendable without over-engineering the first version

**Decision.** The schema includes placeholder fields such as `cost_price` and `supplier_id` for future margin-floor and supplier-catalog features. New triggers follow the same pattern: new domain event, new listener, and new prompt template. New strategies are added as a single `@Component` and automatically become available to the registry.

**Excluded on purpose.** Auth, competitor-aware pricing, auto-approval, durable outbox infrastructure, complex multi-model “jury” voting, and deeper Spring AI integration are deliberately not part of this version. The human acceptance checkpoint already acts as the second opinion layer.

**Tradeoffs.** The design leaves some placeholder concepts in the data model, but this keeps the system extensible without introducing unneeded complexity in the first implementation.

## Other decisions and known limitations

- **Human checkpoint.** Price and stock changes are only applied after an explicit `ACCEPTED` decision. The status update and value update are performed in the same transaction for consistency.
- **PostgreSQL instead of H2.** The application runs against a local PostgreSQL instance on port 5432 for development, using Flyway for schema creation and validation. PostgreSQL is preferred because it is closer to production than H2 and supports the domain's needs more naturally.
- **Frontend stack.** The UI is built with React 18 + Vite and uses plain `fetch` calls. The Vite dev proxy routes `/api` to the backend during development, and backend CORS is configured for the local ports currently used by the app.
- **Velocity tracking.** The product model maintains a rolling 24-hour velocity counter that does not currently decay. The upgrade path is a dedicated event table with timestamps for time-bucketed analysis.
- **Rule-based reorder logic.** The reorder recommendation uses a threshold-based formula that incorporates reorder lead time and current velocity. This keeps reorder suggestions useful even when demand spikes on already well-stocked products.
- **Stale suggestions.** A still-pending price suggestion may remain after a reorder is accepted. This is intentional for the human checkpoint and is treated as a future improvement rather than an immediate functional bug.

## Summary

StockPulse is intentionally designed as a compact, human-in-the-loop decision system. It combines a modular Spring Boot backend, runtime-switchable strategy engine, asynchronous event-driven suggestion generation, and a simple Vite frontend. The design favors correctness, observability, and fast iteration over distributed complexity, which is appropriate for the current scope and a sensible foundation for future scaling.
