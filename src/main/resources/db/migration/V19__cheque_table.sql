CREATE TABLE cheque (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cheque_number      VARCHAR(50) NOT NULL,
    cheque_type        VARCHAR(10) NOT NULL,
    current_account_id UUID NOT NULL REFERENCES current_account (id) ON DELETE RESTRICT,
    bank_name          VARCHAR(100),
    branch             VARCHAR(100),
    amount             NUMERIC(18,2) NOT NULL,
    issue_date         DATE NOT NULL,
    due_date           DATE NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PORTFOLIO',
    bank_account_id    UUID REFERENCES bank_account (id) ON DELETE SET NULL,
    notes              TEXT,
    created_by         VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
