package service;

import enums.Permission;
import enums.Scope;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Stateless
public class AuthorizationService implements AuthorizationServiceLocal {

    @PersistenceContext(unitName = "testPU")
    private EntityManager entityManager;

    @EJB
    private EffectiveRoleResolver effectiveRoleResolver;

    @Override
    public boolean can(String userId, Permission permission) {
        return can(userId, permission, null);
    }

    @Override
    public boolean can(String userId, Permission permission, Scope scope) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(permission, "permission");
        Set<UUID> roleIds = effectiveRoleResolver.resolveEffectiveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        List<UUID> idList = new ArrayList<>(roleIds);
        String placeholders = String.join(",", Collections.nCopies(idList.size(), "?"));
        String sql = """
                SELECT rp.permission::text, r.scope::text
                FROM role_permission rp
                INNER JOIN role r ON r.id = rp.role_id AND r.deleted_at IS NULL
                WHERE rp.role_id IN ("""
                + placeholders
                + ")";
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < idList.size(); i++) {
            query.setParameter(i + 1, idList.get(i));
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        String permName = permission.name();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            String p = row[0] != null ? row[0].toString().trim() : "";
            String s = row[1] != null ? row[1].toString().trim() : "";
            if (!permName.equalsIgnoreCase(p)) {
                continue;
            }
            if (scope == null || scope.name().equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
