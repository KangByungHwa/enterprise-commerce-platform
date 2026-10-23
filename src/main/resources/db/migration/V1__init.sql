CREATE TABLE inventory (
  sku VARCHAR(64) PRIMARY KEY,
  available INT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT chk_inventory_available CHECK (available >= 0)
);
CREATE TABLE purchase_orders (
  id BINARY(16) PRIMARY KEY,
  customer_id VARCHAR(64) NOT NULL,
  sku VARCHAR(64) NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  INDEX idx_orders_customer_created(customer_id, created_at)
);
CREATE TABLE outbox_events (
  id BINARY(16) PRIMARY KEY,
  aggregate_type VARCHAR(80) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  payload TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  INDEX idx_outbox_unpublished(published_at, created_at)
);
INSERT INTO inventory(sku,available,version) VALUES ('SKU-001',100,0),('SKU-002',50,0);

