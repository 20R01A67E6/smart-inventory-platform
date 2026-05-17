# Smart Inventory & Order Fulfillment Platform

A production-grade, event-driven microservices platform built with Java 17, Spring Boot 3, and Apache Kafka. It handles inventory management, order processing, payment coordination, and ML-driven demand forecasting using the Saga orchestration pattern and transactional outbox.

## Architecture

```
                        ┌─────────────┐
                        │ API Gateway │  :8080
                        │ (JWT + Rate  │
                        │  Limiting)  │
                        └──────┬──────┘
             ┌─────────────────┼─────────────────┐
             │                 │                 │
    ┌────────▼──────┐ ┌────────▼──────┐ ┌────────▼──────┐
    │ Order Service │ │  Inventory    │ │   Payment     │
    │    :8081      │ │  Service :8082│ │  Service :8083│
    │  (Saga Orch.) │ │ (Redis cache) │ │ (Resilience4j)│
    └────────┬──────┘ └────────┬──────┘ └────────┬──────┘
             │                 │                 │
             └────────────┬────┘─────────────────┘
                          │  Apache Kafka
                          │  (10 topics)
             ┌────────────┼─────────────────────────┐
             │            │                         │
    ┌─────────▼──────┐  ┌─▼─────────────┐  ┌───────▼────────┐
    │ Notification   │  │  ML Restock   │  │  PostgreSQL x4 │
    │ Service :8084  │  │ Engine :8085  │  │  (per-service) │
    │ (event fanout) │  │ (WMA forecast)│  └────────────────┘
    └────────────────┘  └───────────────┘
```

### Saga Flow (Happy Path)

```
OrderCreated → InventoryReserved → PaymentProcessed → OrderConfirmed
```

On failure, compensating events (`InventoryReleased`, `OrderCancelled`) are published automatically by the saga orchestrator.

### Kafka Topics

| Topic | Producer | Consumers |
|---|---|---|
| `order.created` | order-service | inventory-service, notification-service |
| `order.confirmed` | order-service | notification-service |
| `order.cancelled` | order-service | inventory-service, notification-service |
| `inventory.reserved` | inventory-service | payment-service, notification-service |
| `inventory.reservation.failed` | inventory-service | order-service (saga), notification-service |
| `inventory.released` | inventory-service | notification-service |
| `inventory.low-stock` | inventory-service | ml-restock-engine, notification-service |
| `payment.processed` | payment-service | order-service (saga), notification-service |
| `payment.failed` | payment-service | inventory-service, order-service (saga), notification-service |
| `ml.restock.recommended` | ml-restock-engine | notification-service |

## Services

| Service | Port | Database | Notes |
|---|---|---|---|
| api-gateway | 8080 | — | Spring Cloud Gateway, JWT auth, Redis rate limiter |
| order-service | 8081 | orders_db | Saga orchestrator, transactional outbox |
| inventory-service | 8082 | inventory_db | Redis cache, optimistic locking |
| payment-service | 8083 | payments_db | Resilience4j circuit breaker + retry |
| notification-service | 8084 | — | Kafka fanout, event logging |
| ml-restock-engine | 8085 | ml_db | Weighted Moving Average demand forecasting |

## Getting Started

### Prerequisites

- Docker Desktop 4.x+
- Docker Compose v2+

### Run everything

```bash
docker compose up --build
```

First startup takes ~3 minutes while Maven downloads dependencies and databases initialise via Flyway.

### Service endpoints (via API Gateway)

```
POST   /api/orders                           Create order
GET    /api/orders/{id}                      Get order
GET    /api/products                         List products
GET    /api/products/{id}                    Get product
POST   /api/inventory/products               Create product
POST   /api/payments/process                 Process payment
GET    /api/forecasts/alerts                 Pending restock alerts
GET    /api/forecasts/alerts/{productId}/latest  Latest alert
POST   /api/forecasts/run/{productId}        Trigger manual forecast
```

### Observability

| UI | URL | Credentials |
|---|---|---|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Kafka UI | http://localhost:9080 | — |

### Swagger / OpenAPI

Each service exposes interactive API docs:

- Order Service: http://localhost:8081/swagger-ui.html
- Inventory Service: http://localhost:8082/swagger-ui.html
- Payment Service: http://localhost:8083/swagger-ui.html
- ML Restock Engine: http://localhost:8085/swagger-ui.html

## Key Design Decisions

**Transactional Outbox** — Order service writes Kafka events to a local `outbox_messages` table in the same transaction as the order record, then a scheduler publishes and cleans them. Prevents dual-write inconsistencies.

**Optimistic Locking on Inventory** — The `products` table has a `version` column; concurrent reservation attempts fail fast with a version conflict rather than over-committing stock.

**Weighted Moving Average Forecasting** — The ML engine uses a 7-day WMA over 30-day history to predict daily demand. A restock alert is generated when `days_until_stockout < 14`. Triggered automatically on `inventory.low-stock` events.

**Circuit Breaker on Payments** — Resilience4j opens the circuit after 50% failure rate over a 10-call sliding window, preventing cascading failures from a degraded payment gateway.

## Development

### Build without Docker

```bash
# Build all modules
mvn clean package -DskipTests

# Run a service locally (requires Postgres/Kafka/Redis running)
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar
```

### Run tests

```bash
mvn test
```

### Environment variables

All services accept these environment overrides:

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_USER` | inventory_user | DB username |
| `DB_PASS` | inventory_pass | DB password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | (dev default) | HS256 signing key (min 256 bits) |
| `GRAFANA_PASSWORD` | admin | Grafana admin password |
