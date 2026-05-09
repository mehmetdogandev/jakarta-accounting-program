package facade;

import entity.Role;
import entity.RoleGroup;
import entity.RoleGroupRole;
import facadeLocal.RoleGroupFacadeLocal;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Stateless
public class RoleGroupFacade extends AbstractFacade implements RoleGroupFacadeLocal {

    @Override
    public List<RoleGroup> listActive() {
        return entityManager.createQuery(
                        "SELECT g FROM RoleGroup g WHERE g.deletedAt IS NULL ORDER BY g.title",
                        RoleGroup.class)
                .getResultList();
    }

    @Override
    public RoleGroup findById(UUID id) {
        return entityManager.find(RoleGroup.class, id);
    }

    @Override
    public List<UUID> linkedRoleIds(UUID groupId) {
        return entityManager.createQuery(
                        "SELECT r.role.id FROM RoleGroupRole r WHERE r.roleGroup.id = :g AND r.deletedAt IS NULL",
                        UUID.class)
                .setParameter("g", groupId)
                .getResultList();
    }

    @Override
    public List<Role> listRolesForAssignment() {
        return entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.deletedAt IS NULL ORDER BY r.name",
                        Role.class)
                .getResultList();
    }

    @Override
    public void save(RoleGroup group, List<UUID> roleIds, String actorUserId) {
        boolean isNew = group.getId() == null || findById(group.getId()) == null;
        if (isNew) {
            group.setCreatedBy(actorUserId);
            entityManager.persist(group);
        } else {
            group.setLastUpdatedBy(actorUserId);
            group = entityManager.merge(group);
        }
        entityManager.flush();
        UUID gid = group.getId();

        Instant now = Instant.now();
        entityManager.createQuery(
                        "UPDATE RoleGroupRole r SET r.deletedAt = :t, r.deletedBy = :a, r.lastUpdatedBy = :a "
                                + "WHERE r.roleGroup.id = :g AND r.deletedAt IS NULL")
                .setParameter("t", now)
                .setParameter("a", actorUserId)
                .setParameter("g", gid)
                .executeUpdate();

        if (roleIds != null) {
            for (UUID rid : roleIds) {
                Role roleRef = entityManager.find(Role.class, rid);
                if (roleRef == null || roleRef.getDeletedAt() != null) {
                    continue;
                }
                RoleGroupRole link = new RoleGroupRole();
                link.setRoleGroup(group);
                link.setRole(roleRef);
                link.setCreatedBy(actorUserId);
                link.setLastUpdatedBy(actorUserId);
                entityManager.persist(link);
            }
        }
        entityManager.flush();
    }

    @Override
    public void softDelete(UUID groupId, String actorUserId) {
        RoleGroup g = findById(groupId);
        if (g == null || g.getDeletedAt() != null) {
            return;
        }
        Instant now = Instant.now();
        entityManager.createQuery(
                        "UPDATE RoleGroupRole r SET r.deletedAt = :t, r.deletedBy = :a, r.lastUpdatedBy = :a "
                                + "WHERE r.roleGroup.id = :g AND r.deletedAt IS NULL")
                .setParameter("t", now)
                .setParameter("a", actorUserId)
                .setParameter("g", groupId)
                .executeUpdate();

        g.setDeletedAt(now);
        g.setDeletedBy(actorUserId);
        g.setLastUpdatedBy(actorUserId);
        entityManager.merge(g);
        entityManager.flush();
    }
}
