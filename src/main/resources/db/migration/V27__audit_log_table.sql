CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(36) REFERENCES app_user (id) ON DELETE SET NULL,
    username    VARCHAR(100),
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at_desc ON audit_log (created_at DESC);
