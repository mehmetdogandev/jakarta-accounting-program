package service;

import entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class AuditService {

    private static final Logger LOG = Logger.getLogger(AuditService.class.getName());

    @PersistenceContext(unitName = "testPU")
    private EntityManager entityManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(String userId,
                    String username,
                    String action,
                    String entityType,
                    UUID entityId,
                    String oldValue,
                    String newValue,
                    HttpServletRequest request) {
        String ipAddress = null;
        String userAgent = null;
        if (request != null) {
            ipAddress = request.getRemoteAddr();
            userAgent = request.getHeader("User-Agent");
        }
        insertLogRow(userId, username, normalize(action), entityType, entityId, oldValue, newValue, ipAddress, userAgent);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logLogin(String userId, String username, String ip, String userAgent, boolean success) {
        insertLogRow(userId, username, success ? "LOGIN" : "LOGIN_FAILED", "Auth", null, null, null, ip, userAgent);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logAction(String userId, String username, String action, Object entity) {
        UUID entityId = tryReadUuidId(entity);
        log(userId, username, action, entity == null ? null : entity.getClass().getSimpleName(),
                entityId, null, toJson(entity), null);
    }

    public List<AuditLog> findAll(LocalDate from, LocalDate to, String entityType, String userId) {
        String jpql = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user "
                + "WHERE a.createdAt BETWEEN :from AND :to ";
        if (entityType != null && !entityType.isBlank()) {
            jpql += "AND UPPER(a.entityType) = :entityType ";
        }
        if (userId != null && !userId.isBlank()) {
            jpql += "AND a.user.id = :userId ";
        }
        jpql += "ORDER BY a.createdAt DESC";
        var query = entityManager.createQuery(jpql, AuditLog.class)
                .setParameter("from", from.atStartOfDay().toInstant(ZoneOffset.UTC))
                .setParameter("to", to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        if (entityType != null && !entityType.isBlank()) {
            query.setParameter("entityType", entityType.trim().toUpperCase(Locale.ROOT));
        }
        if (userId != null && !userId.isBlank()) {
            query.setParameter("userId", userId);
        }
        return query.getResultList();
    }

    public List<AuditLog> findByEntity(String entityType, UUID entityId) {
        return entityManager.createQuery(
                        "SELECT a FROM AuditLog a LEFT JOIN FETCH a.user "
                                + "WHERE UPPER(a.entityType) = :entityType AND a.entityId = :entityId "
                                + "ORDER BY a.createdAt DESC",
                        AuditLog.class)
                .setParameter("entityType", entityType == null ? "" : entityType.trim().toUpperCase(Locale.ROOT))
                .setParameter("entityId", entityId)
                .getResultList();
    }

    private String toJson(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            JsonObjectBuilder b = Json.createObjectBuilder();
            for (Method m : entity.getClass().getMethods()) {
                if (m.getParameterCount() != 0) {
                    continue;
                }
                String name = m.getName();
                if (!name.startsWith("get") || "getClass".equals(name)) {
                    continue;
                }
                Object value = m.invoke(entity);
                String key = Introspector.decapitalize(name.substring(3));
                if (value == null) {
                    b.addNull(key);
                } else if (value instanceof Number n) {
                    b.add(key, n.toString());
                } else if (value instanceof Boolean bool) {
                    b.add(key, bool);
                } else {
                    b.add(key, String.valueOf(value));
                }
            }
            return b.build().toString();
        } catch (Exception ex) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    private static String normalize(String action) {
        return action == null ? "UNKNOWN" : action.trim().toUpperCase(Locale.ROOT);
    }

    private static UUID tryReadUuidId(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            Method m = entity.getClass().getMethod("getId");
            Object id = m.invoke(entity);
            if (id instanceof UUID u) {
                return u;
            }
            if (id instanceof String s && !s.isBlank()) {
                return UUID.fromString(s);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void insertLogRow(String userId,
                              String username,
                              String action,
                              String entityType,
                              UUID entityId,
                              String oldValue,
                              String newValue,
                              String ipAddress,
                              String userAgent) {
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO audit_log (id, user_id, username, action, entity_type, entity_id, old_value, new_value, ip_address, user_agent, created_at) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .setParameter(1, UUID.randomUUID())
                    .setParameter(2, userId)
                    .setParameter(3, username)
                    .setParameter(4, normalize(action))
                    .setParameter(5, entityType)
                    .setParameter(6, entityId)
                    .setParameter(7, oldValue)
                    .setParameter(8, newValue)
                    .setParameter(9, ipAddress)
                    .setParameter(10, userAgent)
                    .setParameter(11, Timestamp.from(Instant.now()))
                    .executeUpdate();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Audit insert failed; business flow continues.", ex);
        }
    }
}
