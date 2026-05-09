-- Granular RBAC: one role per (scope, permission); super_admin role group; seed admin via group only.
-- Soft-delete legacy V1/V2 seed roles (SuperAdmin, RoleModuleAdmin, RoleGroupModuleAdmin).

UPDATE app_user
SET email          = 'admin@admin.com',
    password       = 'admin123',
    name           = 'admin',
    surname        = 'user',
    updated_at     = NOW()
WHERE id = '00000000-0000-4000-8000-000000000001';

DELETE FROM user_role
WHERE user_id = '00000000-0000-4000-8000-000000000001';

DELETE FROM user_role_group
WHERE user_id = '00000000-0000-4000-8000-000000000001';

DELETE FROM role_permission
WHERE role_id IN (
    '10000000-0000-4000-8000-000000000001'::uuid,
    '20000000-0000-4000-8000-000000000001'::uuid,
    '30000000-0000-4000-8000-000000000001'::uuid
);

UPDATE role
SET deleted_at = NOW(),
    updated_at = NOW()
WHERE id IN (
    '10000000-0000-4000-8000-000000000001'::uuid,
    '20000000-0000-4000-8000-000000000001'::uuid,
    '30000000-0000-4000-8000-000000000001'::uuid
);

DO $$
DECLARE
    s             app_scope;
    p             app_permission;
    v_role_id     uuid;
    v_group_id    uuid := '40000000-0000-4000-8000-000000000001'::uuid;
    v_admin_id    text := '00000000-0000-4000-8000-000000000001';
BEGIN
    INSERT INTO role_group (id, title, description)
    VALUES (
        v_group_id,
        'super_admin',
        'Tüm scope ve izin rollerini kapsayan varsayılan üst grup'
    );

    FOR s IN SELECT unnest(enum_range(NULL::app_scope))
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

    INSERT INTO user_role_group (user_id, role_group_id)
    VALUES (v_admin_id, v_group_id);
END
$$;
