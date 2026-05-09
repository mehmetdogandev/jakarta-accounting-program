package facade;

import entity.AuditLog;
import facadeLocal.AuditLogFacadeLocal;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Stateless
public class AuditLogFacade extends AbstractFacade implements AuditLogFacadeLocal {

    @Override
    public List<AuditLog> findRecent(int limit) {
        return entityManager.createQuery(
                        "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user ORDER BY a.createdAt DESC",
                        AuditLog.class)
                .setMaxResults(Math.max(1, limit))
                .getResultList();
    }

    @Override
    public List<AuditLog> findByUser(String userId, LocalDate from, LocalDate to) {
        return entityManager.createQuery(
                        "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user "
                                + "WHERE a.user.id = :uid "
                                + "AND a.createdAt BETWEEN :from AND :to "
                                + "ORDER BY a.createdAt DESC",
                        AuditLog.class)
                .setParameter("uid", userId)
                .setParameter("from", from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .setParameter("to", to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .getResultList();
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType) {
        return entityManager.createQuery(
                        "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user "
                                + "WHERE UPPER(a.entityType) = :et "
                                + "ORDER BY a.createdAt DESC",
                        AuditLog.class)
                .setParameter("et", entityType == null ? "" : entityType.trim().toUpperCase())
                .getResultList();
    }

    @Override
    public long countByAction(String action, LocalDate from, LocalDate to) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(a) FROM AuditLog a "
                                + "WHERE UPPER(a.action) = :ac "
                                + "AND a.createdAt BETWEEN :from AND :to",
                        Long.class)
                .setParameter("ac", action == null ? "" : action.trim().toUpperCase())
                .setParameter("from", from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .setParameter("to", to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC))
                .getSingleResult();
        return count == null ? 0L : count;
    }
}
