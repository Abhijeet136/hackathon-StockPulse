# StockPulse

AI inventory and dynamic-pricing **advisor** with a human in the loop.
When stock drops below its threshold or demand spikes, StockPulse notices on its own. It asks an AI (Google Gemini) or deterministic rules for a **price** suggestion and a **reorder** suggestion, then queues both as PENDING. Nothing changes until a merchandiser clicks Accept.

```
Observe (sale -> stock / velocity signal) -> Reason (AI or rules) -> Act (queue PENDING) -> Checkpoint (human accepts or rejects)
```

**Stack:** Spring Boot 3.5 (Java 21), PostgreSQL 18 (local install, Flyway migrations), React 18 + Vite, Gemini `gemini-3.5-flash-lite`.

## Local development setup (no Docker)

Prerequisites: JDK 21, Node 20+, and a local PostgreSQL instance.

### 1. Create the database

Use your local PostgreSQL and create the app database:

```sql
CREATE ROLE root WITH LOGIN PASSWORD 'root';
CREATE DATABASE stockpulse OWNER root;
GRANT ALL PRIVILEGES ON DATABASE stockpulse TO root;
```

The app is configured to connect to `localhost:5432` with the user/password `root/root`.

### 2. Start the backend

```powershell
cd backend
$env:DB_URL = "jdbc:postgresql://localhost:5432/stockpulse"
$env:DB_USER = "root"
$env:DB_PASSWORD = "root"
$env:GEMINI_API_KEY = ""
$env:LLM_MODEL = "gemini-3.5-flash-lite"
.\mvnw.cmd spring-boot:run
```

### 3. Start the frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

Open http://localhost:5173.

- **AI mode:** set `GEMINI_API_KEY` in the backend environment before starting the app.
- **Without a key:** the app still works, and suggestions fall back to rules.
- **Stop:** close the PowerShell windows, or use the local stop script.
- Ports used: 5173 (console), 8080 (API), 5432 (Postgres).

Flyway creates the schema and seeds 9 products on first start.
To reset the demo data: drop the local `stockpulse` database and recreate it, then start again.

Configuration (`backend/src/main/resources/application.yml`, overridable with environment variables):

| Setting | Default | Env var |
|---|---|---|
| `llm.model` | `gemini-3.5-flash-lite` | `LLM_MODEL` |
| `llm.api-key` | *(empty → rule fallback)* | `GEMINI_API_KEY` |
| `llm.provider` | `gemini` (`openai-compatible` also available) | `LLM_PROVIDER` |
| `stockpulse.strategy.pricing` / `.reorder` | `ai` / `ruleBased` | switch at runtime via `PUT /admin/strategy` |
| `stockpulse.spike.multiplier` | `2.0` | |
| datasource | `jdbc:postgresql://localhost:5433/stockpulse` | `DB_URL`, `DB_USER`, `DB_PASSWORD` |

Tests: `cd backend && ./mvnw test` runs 32 tests. They include a Testcontainers Postgres integration test of the whole loop, so Docker must be running.

## API

| Method + path | Purpose |
|---|---|
| `POST /products` | Create product |
| `GET /products?status=&category=` | Filterable list |
| `GET /products/{id}` | One product |
| `PATCH /products/{id}/stock` `{"stockLevel":n}` | Set stock; may fire INVENTORY_LOW |
| `POST /products/{id}/orders` `{"quantity":n}` (body optional, default 1) | Simulate a sale; may fire INVENTORY_LOW / DEMAND_SPIKE |
| `POST /products/{id}/suggest-pricing` | Manual pricing suggestion (synchronous) |
| `POST /products/{id}/suggest-reorder` | Manual reorder suggestion (synchronous) |
| `GET /pricing-suggestions?status=PENDING` | For the console |
| `GET /reorder-suggestions?status=PENDING` | For the console |
| `PATCH /pricing-suggestions/{id}` `{"status":"ACCEPTED"\|"REJECTED"}` | Accept updates the price |
| `PATCH /reorder-suggestions/{id}` `{"status":...}` | Accept adds stock (simulated shipment) |
| `GET / PUT /admin/strategy` `{"pricing":"ai"\|"ruleBased","reorder":...}` | View or switch the active strategy |

Error codes: 404 not found, 409 invalid state change / out of stock / duplicate, 400 validation or unknown strategy. LLM errors never reach the client; they trigger the rule fallback.

## Demo script (5 minutes)

1. **Problem and architecture** (30 s): one Spring Boot modular monolith. Strategy + registry for decisions, events for the agentic loop, and a human checkpoint.
2. **Low stock:** click **Sale** on *Classic Cotton T-Shirt* (PRD-003, stock 8 / threshold 15). The backend log shows an `advisor-` thread. *Inventory low* pricing (AI) and reorder cards appear. Accept the pricing card and reject the seeded *Initial review* card. The price updates and the status returns to Active.
3. **Demand spike:** click **Sell 5** on *Fleece Hoodie* (PRD-008). *Demand spike* cards appear. Click Sell 5 again: no duplicate card.
4. **Runtime switching and resilience:** toggle Pricing between AI and Rules in the header. Restart the backend without `GEMINI_API_KEY` (or with a bad one) and trigger again. A *Rule fallback* card still appears.
5. **ADR:** scroll through [ADR.md](ADR.md) and explain one tradeoff.

## Layout

```
backend/src/main/java/com/stockpulse
├── controller   HTTP only
├── service      ProductService, SuggestionService (coordination), SuggestionWriter (short write transactions)
├── strategy     Pricing/ReorderStrategy, rule-based implementations, StrategyRegistry
├── ai           LLMGateway (Gemini, OpenAI-compatible), PromptBuilder, LlmResponseParser, SuggestionValidator, AI strategies
├── event        InventoryLow/DemandSpike events and @Async listeners
├── domain       Product, PricingSuggestion, ReorderSuggestion, enums
├── repository   Spring Data JPA
├── dto          request/response records
├── exception    GlobalExceptionHandler
└── config       Async, CORS, RestClient, properties
backend/src/main/resources/prompts   six prompt templates (pricing/reorder × low-stock/spike/manual)
backend/src/main/resources/db/migration   Flyway schema + seed
frontend/src     React console (polls every 3 s)
```
