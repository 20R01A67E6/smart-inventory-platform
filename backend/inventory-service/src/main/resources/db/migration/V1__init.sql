-- Inventory Service Database Schema

CREATE TABLE IF NOT EXISTS products (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL,
    sku               VARCHAR(100) NOT NULL UNIQUE,
    description       TEXT,
    price             NUMERIC(19, 4) NOT NULL,
    stock_quantity    INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reorder_threshold INT NOT NULL DEFAULT 10,
    category          VARCHAR(100) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inventory_reservations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL,
    product_id  UUID NOT NULL REFERENCES products(id),
    quantity    INT NOT NULL CHECK (quantity > 0),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_at TIMESTAMPTZ
);

-- Indexes
CREATE INDEX idx_products_sku        ON products(sku);
CREATE INDEX idx_products_category   ON products(category);
CREATE INDEX idx_products_active     ON products(active);
CREATE INDEX idx_reservations_order  ON inventory_reservations(order_id);
CREATE INDEX idx_reservations_status ON inventory_reservations(status);
CREATE INDEX idx_reservations_product ON inventory_reservations(product_id);

-- Seed data for testing
INSERT INTO products (id, name, sku, description, price, stock_quantity, reorder_threshold, category)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Laptop Pro 15', 'LAPTOP-PRO-15',
     '15-inch professional laptop with 32GB RAM', 1299.99, 50, 5, 'Electronics'),
    ('22222222-2222-2222-2222-222222222222', 'Wireless Mouse', 'MOUSE-WL-001',
     'Ergonomic wireless mouse with 90-day battery', 29.99, 200, 20, 'Electronics'),
    ('33333333-3333-3333-3333-333333333333', 'USB-C Hub', 'USBC-HUB-7P',
     '7-in-1 USB-C hub with 4K HDMI', 49.99, 150, 15, 'Electronics'),
    ('44444444-4444-4444-4444-444444444444', 'Standing Desk Mat', 'MAT-STAND-L',
     'Anti-fatigue standing desk mat, large', 79.99, 75, 10, 'Office'),
    ('55555555-5555-5555-5555-555555555555', 'Mechanical Keyboard', 'KB-MECH-TKL',
     'TKL mechanical keyboard, blue switches', 149.99, 8, 10, 'Electronics');

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
