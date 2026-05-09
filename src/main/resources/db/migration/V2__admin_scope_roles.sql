-- Extra scoped roles so admin can open ROLE / ROLE_GROUP menu items (ACCESS + scoped permission checks).
INSERT INTO role (id, name, scope)
VALUES (
        '20000000-0000-4000-8000-000000000001',
        'RoleModuleAdmin',
        'ROLE'
    ),
    (
        '30000000-0000-4000-8000-000000000001',
        'RoleGroupModuleAdmin',
        'ROLE_GROUP'
    );

INSERT INTO role_permission (role_id, permission)
SELECT '20000000-0000-4000-8000-000000000001',
    unnest(enum_range(NULL::app_permission));

INSERT INTO role_permission (role_id, permission)
SELECT '30000000-0000-4000-8000-000000000001',
    unnest(enum_range(NULL::app_permission));

INSERT INTO user_role (id, user_id, role_id)
VALUES (
        gen_random_uuid(),
        '00000000-0000-4000-8000-000000000001',
        '20000000-0000-4000-8000-000000000001'
    ),
    (
        gen_random_uuid(),
        '00000000-0000-4000-8000-000000000001',
        '30000000-0000-4000-8000-000000000001'
    );
