CREATE TABLE product_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id   UUID REFERENCES product_category (id) ON DELETE SET NULL
);

CREATE TABLE product (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(50) NOT NULL UNIQUE,
    barcode            VARCHAR(50),
    name               VARCHAR(255) NOT NULL,
    category_id        UUID REFERENCES product_category (id) ON DELETE SET NULL,
    unit               VARCHAR(20) NOT NULL DEFAULT 'ADET',
    purchase_price     NUMERIC(18,4) NOT NULL DEFAULT 0,
    sales_price        NUMERIC(18,4) NOT NULL DEFAULT 0,
    tax_rate           NUMERIC(5,2) NOT NULL DEFAULT 18,
    stock_quantity     NUMERIC(18,4) NOT NULL DEFAULT 0,
    min_stock_quantity NUMERIC(18,4) NOT NULL DEFAULT 0,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ
);

CREATE TABLE stock_movement (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    UUID NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    movement_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    movement_type VARCHAR(20) NOT NULL,
    quantity      NUMERIC(18,4) NOT NULL,
    unit_cost     NUMERIC(18,4) NOT NULL DEFAULT 0,
    description   TEXT,
    reference_type VARCHAR(30),
    reference_id  UUID,
    created_by    VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);
