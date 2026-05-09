CREATE TABLE expense_category (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(10) NOT NULL,
    description TEXT
);

CREATE TABLE expense (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(10) NOT NULL,
    category_id      UUID REFERENCES expense_category (id) ON DELETE SET NULL,
    transaction_date DATE NOT NULL,
    amount           NUMERIC(18,2) NOT NULL,
    tax_amount       NUMERIC(18,2) NOT NULL DEFAULT 0,
    description      TEXT NOT NULL,
    current_account_id UUID REFERENCES current_account (id) ON DELETE SET NULL,
    payment_method   VARCHAR(20),
    cash_account_id  UUID REFERENCES cash_account (id) ON DELETE SET NULL,
    bank_account_id  UUID REFERENCES bank_account (id) ON DELETE SET NULL,
    receipt_number   VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by      VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    approved_at      TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ
);
