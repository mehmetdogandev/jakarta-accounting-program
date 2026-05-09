CREATE TABLE journal_entry (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_number VARCHAR(20) NOT NULL UNIQUE,
    entry_date   DATE NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_debit  NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_credit NUMERIC(18,2) NOT NULL DEFAULT 0,
    created_by   VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE TABLE journal_entry_line (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id   UUID NOT NULL REFERENCES journal_entry (id) ON DELETE CASCADE,
    line_order         INT NOT NULL,
    account_code       VARCHAR(20) NOT NULL,
    account_name       VARCHAR(255) NOT NULL,
    current_account_id UUID REFERENCES current_account (id) ON DELETE SET NULL,
    description        TEXT,
    debit              NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit             NUMERIC(18,2) NOT NULL DEFAULT 0,
    CHECK (debit >= 0 AND credit >= 0),
    CHECK (NOT (debit > 0 AND credit > 0))
);
