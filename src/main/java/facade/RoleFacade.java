package facade;

import entity.Role;
import enums.Permission;
import enums.Scope;
import facadeLocal.RoleFacadeLocal;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
public class RoleFacade extends AbstractFacade implements RoleFacadeLocal {

    @Override
    @SuppressWarnings("unchecked")
    public List<Role> listByScope(Scope scope) {
        /*
         * PostgreSQL column role.scope is native enum app_scope. EclipseLink binds JPQL enum params as
         * varchar, which fails (operator does not exist: app_scope = character varying). Resolve IDs with
         * a native cast, then load entities by primary key (no enum bind in WHERE).
         */
        List<?> rawIds = entityManager.createNativeQuery(
                        "SELECT id FROM role WHERE scope = CAST(?1 AS app_scope) AND deleted_at IS NULL ORDER BY name")
                .setParameter(1, scope.name())
                .getResultList();
        if (rawIds.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>(rawIds.size());
        for (Object row : rawIds) {
            ids.add(row instanceof UUID u ? u : UUID.fromString(row.toString()));
        }
        List<Role> loaded = entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.id IN :ids ORDER BY r.name", Role.class)
                .setParameter("ids", ids)
                .getResultList();
        return loaded;
    }

    @Override
    public Role findById(UUID id) {
        return entityManager.find(Role.class, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Permission> listPermissions(UUID roleId) {
        List<String> rows = entityManager.createNativeQuery(
                        "SELECT permission::text FROM role_permission WHERE role_id = ?1")
                .setParameter(1, roleId)
                .getResultList();
        List<Permission> out = new ArrayList<>();
        for (String row : rows) {
            try {
                out.add(Permission.valueOf(row.trim()));
            } catch (IllegalArgumentException ignored) {
                // skip unknown
            }
        }
        return out;
    }

    @Override
    public void save(Role role, List<Permission> permissions, String actorUserId) {
        boolean isNew = role.getId() == null || findById(role.getId()) == null;
        if (isNew) {
            role.setCreatedBy(actorUserId);
            entityManager.persist(role);
        } else {
            role.setLastUpdatedBy(actorUserId);
            role = entityManager.merge(role);
        }
        entityManager.flush();
        UUID roleId = role.getId();
        replacePermissions(roleId, permissions);
    }

    private void replacePermissions(UUID roleId, List<Permission> permissions) {
        entityManager.createNativeQuery("DELETE FROM role_permission WHERE role_id = ?1")
                .setParameter(1, roleId)
                .executeUpdate();
        if (permissions == null) {
            return;
        }
        for (Permission p : permissions) {
            entityManager.createNativeQuery(
                            "INSERT INTO role_permission (role_id, permission) VALUES (?1, CAST(?2 AS app_permission))")
                    .setParameter(1, roleId)
                    .setParameter(2, p.name())
                    .executeUpdate();
        }
        entityManager.flush();
    }

    @Override
    public void softDelete(UUID roleId, String actorUserId) {
        Role r = findById(roleId);
        if (r == null || r.getDeletedAt() != null) {
            return;
        }
        Instant now = Instant.now();
        r.setDeletedAt(now);
        r.setDeletedBy(actorUserId);
        r.setLastUpdatedBy(actorUserId);
        entityManager.merge(r);
        entityManager.flush();
    }
}
