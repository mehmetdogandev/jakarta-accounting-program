package service;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves effective {@link entity.Role} ids for a user by unioning direct {@code user_role}
 * assignments with roles implied via {@code user_role_group} → {@code role_group_role}, deduped by role id.
 */
@Stateless
public class EffectiveRoleResolver {

    private static final String EFFECTIVE_ROLE_IDS_SQL = """
            SELECT DISTINCT ur.role_id FROM user_role ur
            INNER JOIN role r ON r.id = ur.role_id AND r.deleted_at IS NULL
            WHERE ur.user_id = ?1 AND ur.deleted_at IS NULL
            UNION
            SELECT DISTINCT rgr.role_id FROM user_role_group urg
            INNER JOIN role_group rg ON rg.id = urg.role_group_id AND rg.deleted_at IS NULL
            INNER JOIN role_group_role rgr ON rgr.role_group_id = rg.id AND rgr.deleted_at IS NULL
            INNER JOIN role r ON r.id = rgr.role_id AND r.deleted_at IS NULL
            WHERE urg.user_id = ?2 AND urg.deleted_at IS NULL
            """;

    @PersistenceContext(unitName = "testPU")
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public Set<UUID> resolveEffectiveRoleIds(String userId) {
        Query query = entityManager.createNativeQuery(EFFECTIVE_ROLE_IDS_SQL);
        query.setParameter(1, userId);
        query.setParameter(2, userId);
        List<Object> rows = query.getResultList();
        Set<UUID> ids = new HashSet<>();
        for (Object row : rows) {
            if (row instanceof UUID u) {
                ids.add(u);
            } else if (row != null) {
                ids.add(UUID.fromString(row.toString()));
            }
        }
        return ids;
    }
}
