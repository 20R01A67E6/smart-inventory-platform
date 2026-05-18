# System Architecture

## Overview

Smart Inventory & Order Fulfillment Platform is an event-driven microservices system built around Apache Kafka. Each service owns its own PostgreSQL database (Database-per-Service pattern). Services communicate exclusively through Kafka events — no synchronous REST calls between backend services.

## Service Inventory

| Service | Port | Language | Database | Role |
|---|---|---|---|---|
| api-gateway | 8080 | Java 17 / Spring Cloud Gateway | — | JWT auth, rate limiting, routing |
| order-service | 8081 | Java 17 / Spring Boot 3 | orders_db | Order lifecycle, saga orchestration, transactional outbox |
| inventory-service | 8082 | Java 17 / Spring Boot 3 | inventory_db | Stock management, pessimistic locking, Redis cache |
| payment-service | 8083 | Java 17 / Spring Boot 3 | payments_db | Payment processing, Resilience4j circuit breaker |
| notification-service | 8084 | Java 17 / Spring Boot 3 | — | Event fan-out, email/webhook dispatch |
| ml-restock-engine | 8085 | Java 17 / Spring Boot 3 | ml_db | Demand forecasting (Weighted Moving Average), restock alerts |
| frontend | 3001 | Next.js 14 / TypeScript | — | React dashboard (TanStack Query, Recharts, Radix UI) |

## Event Flow — Happy Path

```
[Client] → POST /api/orders
              │
              ▼
       [api-gateway :8080]
       JWT auth + rate limit
              │
              ▼
       [order-service :8081]
       1. Persist Order (PENDING)
       2. Write OrderCreatedEvent → outbox_messages (same TX)
       3. OutboxProcessor publishes → Kafka
              │
              ▼ order.created
       [inventory-service :8082]
       4. findByIdWithLock (PESSIMISTIC_WRITE)
       5. Decrement stock, save InventoryReservation
       6. Publish → inventory.reserved
              │
              ▼ inventory.reserved
       [payment-service :8083]
       7. Charge customer (circuit breaker guards gateway)
       8. Publish → payment.processed
              │
              ┌──────────────────┐
              ▼ payment.processed│
       [order-service :8081]    │
       9. Update Order → CONFIRMED
       10. Write OrderConfirmedEvent → outbox → order.confirmed
              │                  │
              ▼ order.confirmed  │ payment.processed
       [notification-service]   [notification-service]
       11. Notify customer       12. Notify payment team
```

## Saga Compensation (Failure Paths)

```
inventory.reservation.failed
        │
        ▼
  [order-service]
  cancelOrder() → outbox → order.cancelled
        │
        ▼
  [inventory-service]
  releaseReservations() → inventory.released

payment.failed
        │
        ┌─────────────────────┐
        ▼                     ▼
  [order-service]     [inventory-service]
  cancelOrder()       releaseReservations()
```

## Transactional Outbox Pattern

Order-service avoids the dual-write problem (DB + Kafka) by writing events to an `outbox_messages` table **in the same transaction** as the business entity. A scheduled `OutboxProcessor` (every 5 s) polls for unprocessed rows and publishes them to Kafka via `KafkaTemplate<String, String>` with synchronous confirmation. This guarantees at-least-once delivery even if Kafka is temporarily unavailable.

```
createOrder() TX:
  INSERT INTO orders …
  INSERT INTO outbox_messages (topic='order.created', payload=JSON)
  COMMIT

OutboxProcessor (every 5s):
  SELECT * FROM outbox_messages WHERE processed = false
  kafkaTemplate.send(topic, key, payload).get()   ← sync
  UPDATE outbox_messages SET processed = true
```

## ML Restock Engine

The ML engine triggers on `inventory.low-stock` events (published by inventory-service when stock drops below `reorder_threshold`). It computes a 7-day Weighted Moving Average over the last 30 days of `sales_history`:

```
WMA = (d₁×1 + d₂×2 + … + d₇×7) / (1+2+…+7)

daysUntilStockout = currentStock / WMA_daily

if daysUntilStockout < 14:
    recommendedQty = WMA_daily × 30 × 1.5   (30-day supply + 50% safety stock)
    publish ml.restock.recommended
```

Confidence score penalises high demand variance: `confidence = 1 - (stdDev / mean)`.

## Infrastructure

| Component | Image | Purpose |
|---|---|---|
| PostgreSQL 16 | postgres:16-alpine | Persistent store for all services |
| Apache Kafka | confluentinc/cp-kafka:7.6.1 | Event backbone |
| Zookeeper | confluentinc/cp-zookeeper:7.6.1 | Kafka cluster coordination |
| Redis 7 | redis:7-alpine | API Gateway rate-limiter store + inventory cache |
| Prometheus | prom/prometheus:v2.52.0 | Metrics scraping (15 s interval) |
| Grafana | grafana/grafana:11.0.0 | Dashboards (auto-provisioned) |
| Kafka UI | provectuslabs/kafka-ui | Topic/consumer-lag inspection |

## Security

- **JWT authentication** enforced at the API Gateway (`JwtAuthFilter`). All downstream services are internal and not exposed.
- **Rate limiting** per user key (via Redis token bucket) on all routes.
- **Non-root containers** — each service image runs as `appuser` in group `appgroup`.
- **Pessimistic locking** on inventory rows prevents overselling under concurrent requests.

## Observability

All Spring Boot services expose `/actuator/prometheus`. Prometheus scrapes every 15 s. The Grafana dashboard (`monitoring/grafana/provisioning/dashboards/inventory-platform.json`) shows:

- Service health (up/down) tiles
- HTTP request rate and P99 latency per service
- JVM heap and thread counts
- Kafka consumer lag
- Payment circuit-breaker state

## Repository Layout

```
smart-inventory-platform/
├── backend/                  Java microservices (Maven multi-module)
│   ├── pom.xml               Parent POM (Spring Boot 3.2.5, Java 17)
│   ├── common/               Shared DTOs, events, Kafka topic constants
│   ├── api-gateway/          Spring Cloud Gateway
│   ├── order-service/        Orders + saga + outbox
│   ├── inventory-service/    Stock + reservations + Redis cache
│   ├── payment-service/      Payments + Resilience4j
│   ├── notification-service/ Event fan-out
│   ├── ml-restock-engine/    Demand forecasting
│   ├── docker-compose.yml    Full local stack
│   ├── monitoring/           Prometheus + Grafana config
│   └── infra/                PostgreSQL init scripts
├── frontend/                 Next.js 14 dashboard
├── docs/
│   └── architecture.md       (this file)
└── .github/workflows/        CI pipelines
```
