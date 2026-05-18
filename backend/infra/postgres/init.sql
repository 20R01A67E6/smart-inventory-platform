-- Create additional databases for each service.
-- The primary database (orders_db) is created by the POSTGRES_DB env var.

CREATE DATABASE inventory_db;
CREATE DATABASE payments_db;
CREATE DATABASE ml_db;

-- Grant privileges to the shared user
GRANT ALL PRIVILEGES ON DATABASE orders_db    TO inventory_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_user;
GRANT ALL PRIVILEGES ON DATABASE payments_db  TO inventory_user;
GRANT ALL PRIVILEGES ON DATABASE ml_db        TO inventory_user;
