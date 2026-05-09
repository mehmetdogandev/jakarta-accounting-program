DO $$
DECLARE
    v_group_id uuid;
    v_role_id_access uuid;
    v_role_id_read uuid;
BEGIN
    SELECT rg.id
    INTO v_group_id
    FROM role_group rg
    WHERE rg.title = 'super_admin'
      AND rg.deleted_at IS NULL
    ORDER BY rg.created_at
    LIMIT 1;

    IF v_group_id IS NULL THEN
        v_group_id := '40000000-0000-4000-8000-000000000001'::uuid;
    END IF;

    v_role_id_access := gen_random_uuid();
    INSERT INTO role (id, name, scope) VALUES (v_role_id_access, 'AUDIT_LOG_ACCESS', 'AUDIT_LOG');
    INSERT INTO role_permission (role_id, permission) VALUES (v_role_id_access, 'ACCESS');
    INSERT INTO role_group_role (role_group_id, role_id) VALUES (v_group_id, v_role_id_access);

    v_role_id_read := gen_random_uuid();
    INSERT INTO role (id, name, scope) VALUES (v_role_id_read, 'AUDIT_LOG_READ', 'AUDIT_LOG');
    INSERT INTO role_permission (role_id, permission) VALUES (v_role_id_read, 'READ');
    INSERT INTO role_group_role (role_group_id, role_id) VALUES (v_group_id, v_role_id_read);
END
$$;
