CREATE TABLE cash_transaction (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cash_account_id   UUID NOT NULL REFERENCES cash_account (id) ON DELETE CASCADE,
    transaction_date  DATE NOT NULL,
    transaction_type  VARCHAR(10) NOT NULL,
    amount            NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    description       TEXT,
    current_account_id UUID REFERENCES current_account (id) ON DELETE SET NULL,
    reference_type    VARCHAR(30),
    reference_id      UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE TABLE bank_transaction (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_account_id   UUID NOT NULL REFERENCES bank_account (id) ON DELETE CASCADE,
    transaction_date  DATE NOT NULL,
    transaction_type  VARCHAR(10) NOT NULL,
    amount            NUMERIC(18,2) NOT NULL CHECK (amount > 0),
    description       TEXT,
    current_account_id UUID REFERENCES current_account (id) ON DELETE SET NULL,
    reference_type    VARCHAR(30),
    reference_id      UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);
