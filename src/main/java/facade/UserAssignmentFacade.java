package facade;

import entity.Role;
import entity.RoleGroup;
import entity.UserRole;
import entity.UserRoleGroup;
import facadeLocal.UserAssignmentFacadeLocal;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class UserAssignmentFacade extends AbstractFacade implements UserAssignmentFacadeLocal {

    private static String likePattern(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String cleaned = query.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return "%" + cleaned + "%";
    }

    @Override
    public List<Role> listAssignedRoles(String userId) {
        return entityManager.createQuery(
                        "SELECT r FROM UserRole ur JOIN ur.role r "
                                + "WHERE ur.userId = :uid AND ur.deletedAt IS NULL AND r.deletedAt IS NULL "
                                + "ORDER BY r.scope, r.name",
                        Role.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Override
    public List<RoleGroup> listAssignedRoleGroups(String userId) {
        return entityManager.createQuery(
                        "SELECT g FROM UserRoleGroup urg JOIN urg.roleGroup g "
                                + "WHERE urg.userId = :uid AND urg.deletedAt IS NULL AND g.deletedAt IS NULL "
                                + "ORDER BY g.title",
                        RoleGroup.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Override
    public List<Role> searchRolesForPicker(String userId, String query, int first, int pageSize) {
        TypedQuery<Role> q = entityManager.createQuery(
                        "SELECT r FROM Role r WHERE r.deletedAt IS NULL AND LOWER(r.name) LIKE :q "
                                + "AND NOT EXISTS (SELECT 1 FROM UserRole ur WHERE ur.userId = :uid "
                                + "AND ur.role.id = r.id AND ur.deletedAt IS NULL) "
                                + "ORDER BY r.scope, r.name",
                        Role.class);
        q.setParameter("q", likePattern(query));
        q.setParameter("uid", userId);
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    @Override
    public long countRolesForPicker(String userId, String query) {
        Long n = entityManager.createQuery(
                        "SELECT COUNT(r) FROM Role r WHERE r.deletedAt IS NULL AND LOWER(r.name) LIKE :q "
                                + "AND NOT EXISTS (SELECT 1 FROM UserRole ur WHERE ur.userId = :uid "
                                + "AND ur.role.id = r.id AND ur.deletedAt IS NULL)",
                        Long.class)
                .setParameter("q", likePattern(query))
                .setParameter("uid", userId)
                .getSingleResult();
        return n != null ? n : 0L;
    }

    @Override
    public List<RoleGroup> searchRoleGroupsForPicker(String userId, String query, int first, int pageSize) {
        TypedQuery<RoleGroup> q = entityManager.createQuery(
                        "SELECT g FROM RoleGroup g WHERE g.deletedAt IS NULL "
                                + "AND (LOWER(g.title) LIKE :q OR LOWER(COALESCE(g.description, '')) LIKE :q) "
                                + "AND NOT EXISTS (SELECT 1 FROM UserRoleGroup urg WHERE urg.userId = :uid "
                                + "AND urg.roleGroup.id = g.id AND urg.deletedAt IS NULL) "
                                + "ORDER BY g.title",
                        RoleGroup.class);
        q.setParameter("q", likePattern(query));
        q.setParameter("uid", userId);
        q.setFirstResult(first);
        q.setMaxResults(pageSize);
        return q.getResultList();
    }

    @Override
    public long countRoleGroupsForPicker(String userId, String query) {
        Long n = entityManager.createQuery(
                        "SELECT COUNT(g) FROM RoleGroup g WHERE g.deletedAt IS NULL "
                                + "AND (LOWER(g.title) LIKE :q OR LOWER(COALESCE(g.description, '')) LIKE :q) "
                                + "AND NOT EXISTS (SELECT 1 FROM UserRoleGroup urg WHERE urg.userId = :uid "
                                + "AND urg.roleGroup.id = g.id AND urg.deletedAt IS NULL)",
                        Long.class)
                .setParameter("q", likePattern(query))
                .setParameter("uid", userId)
                .getSingleResult();
        return n != null ? n : 0L;
    }

    @Override
    public void assignRole(String userId, UUID roleId, String actorUserId) {
        Role role = entityManager.find(Role.class, roleId);
        if (role == null || role.getDeletedAt() != null) {
            return;
        }
        List<UserRole> existing = entityManager.createQuery(
                        "SELECT ur FROM UserRole ur WHERE ur.userId = :u AND ur.role.id = :rid",
                        UserRole.class)
                .setParameter("u", userId)
                .setParameter("rid", roleId)
                .getResultList();
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRole(role);
            ur.setCreatedBy(actorUserId);
            ur.setLastUpdatedBy(actorUserId);
            entityManager.persist(ur);
            entityManager.flush();
            return;
        }
        for (UserRole ur : existing) {
            if (ur.getDeletedAt() == null) {
                return;
            }
            ur.setDeletedAt(null);
            ur.setDeletedBy(null);
            ur.setLastUpdatedBy(actorUserId);
            ur.setUpdatedAt(now);
            entityManager.merge(ur);
        }
        entityManager.flush();
    }

    @Override
    public void removeRoleAssignment(String userId, UUID roleId, String actorUserId) {
        List<UserRole> list = entityManager.createQuery(
                        "SELECT ur FROM UserRole ur WHERE ur.userId = :u AND ur.role.id = :rid AND ur.deletedAt IS NULL",
                        UserRole.class)
                .setParameter("u", userId)
                .setParameter("rid", roleId)
                .getResultList();
        Instant now = Instant.now();
        for (UserRole ur : list) {
            ur.setDeletedAt(now);
            ur.setDeletedBy(actorUserId);
            ur.setLastUpdatedBy(actorUserId);
            entityManager.merge(ur);
        }
        entityManager.flush();
    }

    @Override
    public void assignRoleGroup(String userId, UUID roleGroupId, String actorUserId) {
        RoleGroup group = entityManager.find(RoleGroup.class, roleGroupId);
        if (group == null || group.getDeletedAt() != null) {
            return;
        }
        List<UserRoleGroup> existing = entityManager.createQuery(
                        "SELECT urg FROM UserRoleGroup urg WHERE urg.userId = :u AND urg.roleGroup.id = :gid",
                        UserRoleGroup.class)
                .setParameter("u", userId)
                .setParameter("gid", roleGroupId)
                .getResultList();
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            UserRoleGroup urg = new UserRoleGroup();
            urg.setUserId(userId);
            urg.setRoleGroup(group);
            urg.setCreatedBy(actorUserId);
            urg.setLastUpdatedBy(actorUserId);
            entityManager.persist(urg);
            entityManager.flush();
            return;
        }
        for (UserRoleGroup urg : existing) {
            if (urg.getDeletedAt() == null) {
                return;
            }
            urg.setDeletedAt(null);
            urg.setDeletedBy(null);
            urg.setLastUpdatedBy(actorUserId);
            urg.setUpdatedAt(now);
            entityManager.merge(urg);
        }
        entityManager.flush();
    }

    @Override
    public void removeRoleGroupAssignment(String userId, UUID roleGroupId, String actorUserId) {
        List<UserRoleGroup> list = entityManager.createQuery(
                        "SELECT urg FROM UserRoleGroup urg WHERE urg.userId = :u AND urg.roleGroup.id = :gid "
                                + "AND urg.deletedAt IS NULL",
                        UserRoleGroup.class)
                .setParameter("u", userId)
                .setParameter("gid", roleGroupId)
                .getResultList();
        Instant now = Instant.now();
        for (UserRoleGroup urg : list) {
            urg.setDeletedAt(now);
            urg.setDeletedBy(actorUserId);
            urg.setLastUpdatedBy(actorUserId);
            entityManager.merge(urg);
        }
        entityManager.flush();
    }
}
