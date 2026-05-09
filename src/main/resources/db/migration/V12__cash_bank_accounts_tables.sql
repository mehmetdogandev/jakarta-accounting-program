CREATE TABLE cash_account (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code       VARCHAR(20) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    currency   VARCHAR(3) NOT NULL DEFAULT 'TRY',
    balance    NUMERIC(18,2) NOT NULL DEFAULT 0,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bank_account (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(20) NOT NULL UNIQUE,
    bank_name      VARCHAR(100) NOT NULL,
    branch         VARCHAR(100),
    account_number VARCHAR(50),
    iban           VARCHAR(34),
    currency       VARCHAR(3) NOT NULL DEFAULT 'TRY',
    balance        NUMERIC(18,2) NOT NULL DEFAULT 0,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
