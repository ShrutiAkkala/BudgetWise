# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot — run from `backend/`)
```bash
./mvnw spring-boot:run          # Start the backend (port 8080)
./mvnw test                     # Run all tests
./mvnw test -Dtest=ClassName    # Run a single test class
./mvnw package -DskipTests      # Build JAR
./mvnw compile                  # Compile only
```

### Frontend (React — run from `frontend/`)
```bash
npm start       # Start dev server (port 3000)
npm test        # Run tests (watch mode)
npm run build   # Production build
```

### Prerequisites
- PostgreSQL running on `localhost:5432` with a database named `budgettracker`
- Credentials and Plaid/Anthropic API keys configured in `backend/src/main/resources/application.yml`

---

## Architecture

This is a full-stack personal finance app with a **React frontend**, **Spring Boot backend**, **PostgreSQL database**, **Plaid** for bank account linking, and **Anthropic Claude** for AI-powered financial chat.

### Backend — `backend/src/main/java/com/budget/tracker/`

Organized as a standard layered Spring Boot app:

| Package | Purpose |
|---|---|
| `controller/` | REST endpoints: `AuthController`, `TransactionController`, `PlaidController`, `AiController` |
| `service/` | Business logic: `AuthService`, `TransactionService`, `PlaidService`, `ClaudeService`, `JwtService` |
| `model/` | JPA entities: `User`, `Transaction`, `BankAccount` + enums `Category`, `TransactionType`, `Role` |
| `repository/` | Spring Data JPA repositories for each entity |
| `dto/` | Request/response DTOs (separate from entities) |
| `config/` | `SecurityConfig`, `PlaidConfig`, `CorsConfig` |
| `filter/` | `JwtAuthenticationFilter` — validates JWT on every incoming request |
| `exception/` | Global exception handler |

**Auth flow:** Stateless JWT (24-hour expiration). The filter extracts the token, validates it via `JwtService`, and sets `SecurityContext`. All protected endpoints receive `@AuthenticationPrincipal User` to scope data to the authenticated user.

**AI chat (RAG):** `ClaudeService` fetches the user's last 3 months of transactions from the DB, builds a structured financial context (income, expenses, categories, recent items), and sends it along with the user's question to the Anthropic API (`claude-opus-4-6`). No vector store — context is built from live DB queries.

**Plaid sync:** `PlaidService` handles the full Plaid Link flow (link token → public token exchange → access token storage). Transactions are synced with cursor-based pagination and stored with `plaidTransactionId` + `plaidImported` flag to avoid duplicates.

### Frontend — `frontend/src/`

| Folder | Purpose |
|---|---|
| `pages/` | Top-level route components: Dashboard, Login, Register, Transactions, Banks |
| `components/` | UI building blocks (charts via Recharts, `BankConnectSection` for Plaid Link, `AiChat`) |
| `api/` | Axios wrappers: `axiosInstance` (JWT interceptor + 401 logout), `authApi`, `transactionApi`, `plaidApi` |
| `context/` | `AuthContext` — global auth state (JWT token, user) stored in `localStorage` |
| `utils/` | `dateHelpers`, `formatCurrency` |
| `__tests__/` | Jest tests for components, context, pages, and utils |

**Auth:** `AuthContext` provides login/logout and the current user globally. `ProtectedRoute` wraps private routes and redirects unauthenticated users to `/login`. The Axios instance automatically attaches the JWT header to every request and redirects to `/login` on 401.

### Frontend → Backend Connection
- Frontend dev server proxies API calls to `http://localhost:8080` (configured in `package.json` or `axiosInstance` base URL)
- All requests carry `Authorization: Bearer <token>` header via the Axios interceptor
- JWT is stored in and read from `localStorage`
