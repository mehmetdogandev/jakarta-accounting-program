DO $$
DECLARE
    s          app_scope;
    p          app_permission;
    v_role_id  uuid;
    v_group_id uuid;
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

    FOR s IN SELECT unnest(ARRAY['CASH_ACCOUNT'::app_scope, 'BANK_ACCOUNT'::app_scope])
        LOOP
            FOR p IN SELECT unnest(enum_range(NULL::app_permission))
                LOOP
                    v_role_id := gen_random_uuid();

                    INSERT INTO role (id, name, scope)
                    VALUES (v_role_id, s::text || '_' || p::text, s);

                    INSERT INTO role_permission (role_id, permission)
                    VALUES (v_role_id, p);

                    INSERT INTO role_group_role (role_group_id, role_id)
                    VALUES (v_group_id, v_role_id);
                END LOOP;
        END LOOP;
END
$$;
