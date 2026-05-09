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
    public List<Role> listByScope(Scope scope) {
        return entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.scope = :s AND r.deletedAt IS NULL ORDER BY r.name",
                        Role.class)
                .setParameter("s", scope)
                .getResultList();
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
