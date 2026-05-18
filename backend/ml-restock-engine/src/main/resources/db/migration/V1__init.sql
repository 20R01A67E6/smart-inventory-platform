-- ML Restock Engine Database Schema

CREATE TABLE IF NOT EXISTS sales_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id     UUID NOT NULL,
    product_name   VARCHAR(255) NOT NULL,
    sale_date      DATE NOT NULL,
    quantity_sold  INT NOT NULL CHECK (quantity_sold > 0),
    order_id       UUID
);

CREATE TABLE IF NOT EXISTS restock_alerts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id           UUID NOT NULL,
    product_name         VARCHAR(255) NOT NULL,
    current_stock        INT NOT NULL,
    recommended_quantity INT NOT NULL CHECK (recommended_quantity > 0),
    days_until_stockout  DOUBLE PRECISION NOT NULL,
    avg_daily_demand     DOUBLE PRECISION NOT NULL,
    confidence_score     DOUBLE PRECISION NOT NULL CHECK (confidence_score BETWEEN 0 AND 1),
    acknowledged         BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for forecasting queries
CREATE INDEX idx_sales_product_date ON sales_history(product_id, sale_date);
CREATE INDEX idx_sales_date         ON sales_history(sale_date);

-- Indexes for alert queries
CREATE INDEX idx_alerts_product     ON restock_alerts(product_id);
CREATE INDEX idx_alerts_ack_stockout ON restock_alerts(acknowledged, days_until_stockout)
    WHERE acknowledged = FALSE;

-- Seed historical sales data aligned with inventory-service product IDs
INSERT INTO sales_history (product_id, product_name, sale_date, quantity_sold, order_id) VALUES
-- Laptop Pro 15 (approaching low stock of 50, threshold 5)
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 1,  2, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 2,  1, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 3,  3, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 5,  2, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 7,  1, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 10, 4, gen_random_uuid()),
('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', CURRENT_DATE - 14, 2, gen_random_uuid()),
-- Mechanical Keyboard (stock 8, threshold 10 — already below threshold)
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 1,  3, gen_random_uuid()),
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 2,  5, gen_random_uuid()),
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 3,  2, gen_random_uuid()),
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 4,  4, gen_random_uuid()),
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 5,  6, gen_random_uuid()),
('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', CURRENT_DATE - 7,  3, gen_random_uuid()),
-- Wireless Mouse (stock 200, steady demand)
('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', CURRENT_DATE - 1,  10, gen_random_uuid()),
('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', CURRENT_DATE - 2,  8,  gen_random_uuid()),
('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', CURRENT_DATE - 3,  12, gen_random_uuid()),
('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', CURRENT_DATE - 5,  9,  gen_random_uuid()),
('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', CURRENT_DATE - 7,  11, gen_random_uuid());
