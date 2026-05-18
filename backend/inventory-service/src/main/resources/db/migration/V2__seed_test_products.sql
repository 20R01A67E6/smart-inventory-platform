-- Additional test products for development/integration testing.
-- Uses INSERT ... ON CONFLICT DO NOTHING so re-running is safe.

INSERT INTO products (id, name, sku, description, price, stock_quantity, reorder_threshold, category)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'Test Widget Alpha',   'TEST-WIDGET-A',
     'Standard test product for order-flow validation', 9.99,  100, 10, 'Test'),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'Test Widget Beta',    'TEST-WIDGET-B',
     'Secondary test product for multi-item orders',    19.99, 200, 20, 'Test'),
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Test Gadget Gamma',   'TEST-GADGET-G',
     'Test product with low reorder threshold',         49.99,  50,  5, 'Test'),
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'Test Accessory Delta','TEST-ACCESS-D',
     'High-volume test product for bulk order testing', 4.99,  500, 50, 'Test')
ON CONFLICT (id) DO NOTHING;
