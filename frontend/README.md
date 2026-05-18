# Smart Inventory — Admin Dashboard

Production-grade Next.js 14 admin dashboard for the Smart Inventory Management Platform.

## Tech Stack

- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS** + dark theme (#0f172a)
- **shadcn/ui** components (Radix UI primitives)
- **Recharts** for data visualization
- **TanStack Query v5** for server state

## Prerequisites

Ensure all backend microservices are running:

| Service | Port | Base URL |
|---|---|---|
| Order Service | 8081 | http://localhost:8081 |
| Inventory Service | 8082 | http://localhost:8082 |
| Payment Service | 8083 | http://localhost:8083 |
| ML Restock Service | 8085 | http://localhost:8085 |

## Setup

```bash
# 1. Install dependencies
npm install

# 2. Configure environment (defaults already in .env.local)
#    Edit .env.local if services run on different ports/hosts
cat .env.local

# 3. Start the development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Pages

| Route | Description |
|---|---|
| `/` | Dashboard — KPIs, order status chart, recent orders, low stock alerts |
| `/orders` | Orders table with status filter, click-through saga timeline, create order modal |
| `/inventory` | Product cards with stock-level indicators and restock actions |
| `/forecasts` | ML restock predictions with days-until-stockout and confidence scores |

## API Proxy

All API calls use Next.js server-side rewrites (configured in `next.config.ts`) so there are no CORS issues in the browser. The frontend calls relative paths like `/api/orders` which Next.js proxies to `http://localhost:8081/api/orders`.

To point to different backends, update `.env.local`:

```env
ORDER_SERVICE_URL=http://your-order-service
INVENTORY_SERVICE_URL=http://your-inventory-service
PAYMENT_SERVICE_URL=http://your-payment-service
FORECAST_SERVICE_URL=http://your-forecast-service
```

## Production Build

```bash
npm run build
npm start
```

## Project Structure

```
src/
├── app/                  # Next.js App Router pages
│   ├── layout.tsx        # Root layout (providers, sidebar, toaster)
│   ├── page.tsx          # Dashboard
│   ├── orders/page.tsx   # Orders management
│   ├── inventory/page.tsx# Inventory management
│   └── forecasts/page.tsx# ML forecasts
├── components/
│   ├── ui/               # shadcn/ui base components
│   ├── layout/           # Sidebar, MainLayout
│   ├── dashboard/        # KPICards, charts, tables
│   ├── orders/           # OrdersTable, CreateOrderModal, SagaTimeline
│   ├── inventory/        # ProductCard, RestockModal, AddProductModal
│   └── forecasts/        # ForecastsTable
├── hooks/                # TanStack Query hooks per service
├── lib/                  # api.ts, queryClient.ts, utils.ts
├── providers/            # QueryProvider
└── types/                # Shared TypeScript types
```
