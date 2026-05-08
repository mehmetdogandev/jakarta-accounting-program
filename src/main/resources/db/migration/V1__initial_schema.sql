-- Enums (match Drizzle semantics; permissions modeled via role_permission for JPA)
CREATE TYPE app_permission AS ENUM (
    'CREATE',
    'READ',
    'ACCESS',
    'UPDATE',
    'DELETE',
    'EXPORT',
    'IMPORT',
    'APPROVE',
    'REJECT',
    'ARCHIVE'
);

CREATE TYPE app_scope AS ENUM (
    'USER',
    'ROLE',
    'ROLE_GROUP'
);

-- Core user (avoid reserved word user → app_user)
CREATE TABLE app_user (
    id               VARCHAR(36) PRIMARY KEY,
    email            VARCHAR(320) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    name             VARCHAR(255),
    surname          VARCHAR(255),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_app_user_email_active ON app_user (email)
    WHERE deleted_at IS NULL;

-- Better-auth style tables (quoted session → parity with Drizzle table name)
CREATE TABLE "session" (
    id                  VARCHAR(255) PRIMARY KEY,
    expires_at          TIMESTAMPTZ NOT NULL,
    token               VARCHAR(512) NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address          VARCHAR(255),
    user_agent          VARCHAR(1024),
    mac_address         VARCHAR(255),
    device_local_ip     VARCHAR(255),
    device_global_ip    VARCHAR(255),
    user_id             VARCHAR(36) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    created_by          VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by     VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by          VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE TABLE account (
    id                         VARCHAR(255) PRIMARY KEY,
    account_id                 VARCHAR(255) NOT NULL,
    provider_id                VARCHAR(255) NOT NULL,
    user_id                    VARCHAR(36) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    access_token               TEXT,
    refresh_token              TEXT,
    id_token                   TEXT,
    access_token_expires_at    TIMESTAMPTZ,
    refresh_token_expires_at   TIMESTAMPTZ,
    scope                      TEXT,
    password                   VARCHAR(255),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                 VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by            VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by                 VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE TABLE verification (
    id               VARCHAR(255) PRIMARY KEY,
    identifier       VARCHAR(512) NOT NULL,
    value            TEXT NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    otp_code         VARCHAR(64),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

-- RBAC
CREATE TABLE role_group (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            TEXT NOT NULL,
    description      TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_role_group_title ON role_group (title)
    WHERE deleted_at IS NULL;

CREATE TABLE role (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             TEXT NOT NULL,
    scope            app_scope NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_role_name_scope ON role (name, scope)
    WHERE deleted_at IS NULL;

CREATE TABLE role_permission (
    role_id      UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    permission   app_permission NOT NULL,
    PRIMARY KEY (role_id, permission)
);

CREATE TABLE user_role_group (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          VARCHAR(36) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_group_id    UUID NOT NULL REFERENCES role_group (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_user_role_group ON user_role_group (user_id, role_group_id)
    WHERE deleted_at IS NULL;

CREATE TABLE role_group_role (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_group_id    UUID NOT NULL REFERENCES role_group (id) ON DELETE CASCADE,
    role_id          UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_role_group_role ON role_group_role (role_group_id, role_id)
    WHERE deleted_at IS NULL;

CREATE TABLE user_role (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          VARCHAR(36) NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id          UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    created_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    last_updated_by  VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    deleted_by       VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX unique_user_role ON user_role (user_id, role_id)
    WHERE deleted_at IS NULL;

-- Legacy demo: student linked to app_user (for existing JSF sample)
CREATE TABLE student (
    id              BIGSERIAL PRIMARY KEY,
    student_number  VARCHAR(255),
    user_id         VARCHAR(36) REFERENCES app_user (id)
);

-- Seed: admin user + full-access role + assignment
INSERT INTO app_user (id, email, password, name, surname)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'admin@example.com',
    'admin',
    'Admin',
    'User'
);

INSERT INTO role (id, name, scope)
VALUES (
    '10000000-0000-4000-8000-000000000001',
    'SuperAdmin',
    'USER'
);

INSERT INTO role_permission (role_id, permission)
SELECT '10000000-0000-4000-8000-000000000001', unnest(enum_range(NULL::app_permission));

INSERT INTO user_role (id, user_id, role_id)
VALUES (
    gen_random_uuid(),
    '00000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000001'
);
