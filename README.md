# Smart Inventory & Order Fulfillment Platform

A production-grade, event-driven microservices platform with a Next.js dashboard. Orders flow through an Apache Kafka-backed saga, inventory is managed with pessimistic locking and Redis caching, payments use a Resilience4j circuit breaker, and an ML engine predicts restock needs using Weighted Moving Average demand forecasting.

## Repository Structure

```
smart-inventory-platform/
├── backend/                  Java 17 · Spring Boot 3 · Apache Kafka
│   ├── common/               Shared events, DTOs, Kafka topic constants
│   ├── api-gateway/          :8080  JWT auth · Redis rate limiting
│   ├── order-service/        :8081  Orders · Saga orchestration · Outbox
│   ├── inventory-service/    :8082  Stock · Pessimistic lock · Redis cache
│   ├── payment-service/      :8083  Payments · Circuit breaker
│   ├── notification-service/ :8084  Event fan-out
│   ├── ml-restock-engine/    :8085  WMA demand forecasting
│   ├── docker-compose.yml    Full local stack
│   └── monitoring/           Prometheus + Grafana
├── frontend/                 Next.js 14 · TypeScript · Tailwind :3001
├── docs/
│   └── architecture.md       Detailed system design
└── .github/workflows/        Backend CI · Frontend CI
```

## Architecture

```
                           ┌─────────────────────┐
[Browser :3001] ──────────▶│    Next.js Frontend  │
                           │  TanStack Query      │
                           │  Recharts · Radix UI │
                           └──────────┬──────────┘
                                      │ REST
                           ┌──────────▼──────────┐
                           │     API Gateway      │ :8080
                           │  JWT · Rate Limit    │
                           └──────────┬──────────┘
              ┌────────────────────────┼───────────────────────┐
              │                        │                        │
   ┌──────────▼──────┐    ┌────────────▼──────┐    ┌──────────▼──────┐
   │  Order Service  │    │Inventory Service  │    │Payment Service  │
   │    :8081        │    │    :8082          │    │    :8083        │
   │  Saga · Outbox  │    │ Pessimistic Lock  │    │ Circuit Breaker │
   │  orders_db      │    │ Redis Cache       │    │ payments_db     │
   └──────────┬──────┘    │ inventory_db      │    └─────────────────┘
              │           └───────────────────┘
              │
              └─────────────────── Apache Kafka ─────────────────────┐
                                  (10 topics)                        │
                         ┌──────────────────┐   ┌───────────────────▼───┐
                         │Notification Svc  │   │  ML Restock Engine    │
                         │    :8084         │   │      :8085            │
                         │  Event fan-out   │   │  WMA Forecasting      │
                         └──────────────────┘   │  ml_db                │
                                                └───────────────────────┘
```

### Saga Flow (Happy Path)

```
order.created → inventory.reserved → payment.processed → order.confirmed
```

Compensation on failure:

```
inventory.reservation.failed → order.cancelled → inventory.released
payment.failed               → order.cancelled → inventory.released
```

## Tech Stack

| Layer | Technology |
|---|---|
| API Gateway | Spring Cloud Gateway 2023.0.1 |
| Backend services | Java 17 · Spring Boot 3.2.5 |
| Event streaming | Apache Kafka 7.6.1 (Confluent) |
| Databases | PostgreSQL 16 (per-service) |
| Cache / Rate limit | Redis 7 |
| Resilience | Resilience4j (circuit breaker + retry) |
| ML / Forecasting | Custom WMA — pure Java |
| Frontend | Next.js 14 · React 18 · TypeScript 5 |
| Styling | Tailwind CSS 3 · Radix UI |
| Charts | Recharts 2 |
| Data fetching | TanStack Query 5 |
| Observability | Prometheus · Grafana 11 |
| Build | Maven 3 (multi-module) · npm |
| Container | Docker Compose v2 |

## Running Locally

### Prerequisites

- Docker Desktop 4.x+ with Compose v2
- 8 GB RAM recommended (10 containers)

### Start everything

```bash
cd backend
docker compose up --build
```

First boot takes ~3 minutes while Maven resolves dependencies and Flyway runs migrations. Services start in dependency order (Postgres → Kafka → Redis → services → observability).

### Frontend development server

```bash
cd frontend
npm install
npm run dev          # http://localhost:3001
```

> The frontend expects the API Gateway at `http://localhost:8080`. Set `NEXT_PUBLIC_API_URL` in `frontend/.env.local` to override.

## API Endpoints (via Gateway on :8080)

### Orders

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders/{id}` | Get order by ID |
| `GET` | `/api/orders/customer/{customerId}` | List orders for a customer |

### Inventory

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/products` | List all active products |
| `GET` | `/api/products/{id}` | Get product by ID |
| `POST` | `/api/inventory/products` | Create product |
| `PUT` | `/api/inventory/products/{id}/stock` | Update stock level |

### Payments

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/payments/process` | Process payment |
| `GET` | `/api/payments/{orderId}` | Get payment for order |

### ML Forecasts

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/forecasts/alerts` | List pending restock alerts |
| `GET` | `/api/forecasts/alerts/{productId}/latest` | Latest alert for product |
| `POST` | `/api/forecasts/run/{productId}` | Trigger manual forecast |

## Observability

| UI | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Kafka UI | http://localhost:9080 | — |
| Swagger (order) | http://localhost:8081/swagger-ui.html | — |
| Swagger (inventory) | http://localhost:8082/swagger-ui.html | — |
| Swagger (payment) | http://localhost:8083/swagger-ui.html | — |
| Swagger (ML) | http://localhost:8085/swagger-ui.html | — |

## Development

```bash
# Build all backend modules (skip tests)
cd backend && mvn clean package -DskipTests

# Run all backend tests
cd backend && mvn test

# Lint frontend
cd frontend && npm run lint

# Type-check frontend
cd frontend && npx tsc --noEmit
```

## Seed Products (for testing)

| Product ID | Name | Stock |
|---|---|---|
| `11111111-1111-1111-1111-111111111111` | Laptop Pro 15 | 50 |
| `22222222-2222-2222-2222-222222222222` | Wireless Mouse | 200 |
| `33333333-3333-3333-3333-333333333333` | USB-C Hub | 150 |
| `44444444-4444-4444-4444-444444444444` | Standing Desk Mat | 75 |
| `55555555-5555-5555-5555-555555555555` | Mechanical Keyboard | 8 |
| `a1b2c3d4-e5f6-7890-abcd-ef1234567890` | Test Widget Alpha | 100 |

See [`docs/architecture.md`](docs/architecture.md) for the full system design.
