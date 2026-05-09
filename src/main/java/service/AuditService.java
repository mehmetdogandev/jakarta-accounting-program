package service;

import entity.AppUser;
import entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class AuditService {

    @PersistenceContext(unitName = "testPU")
    private EntityManager entityManager;

    public void log(String userId,
                    String username,
                    String action,
                    String entityType,
                    UUID entityId,
                    String oldValue,
                    String newValue,
                    HttpServletRequest request) {
        AuditLog row = new AuditLog();
        if (userId != null && !userId.isBlank()) {
            AppUser user = entityManager.find(AppUser.class, userId);
            row.setUser(user);
        }
        row.setUsername(username);
        row.setAction(normalize(action));
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setOldValue(oldValue);
        row.setNewValue(newValue);
        if (request != null) {
            row.setIpAddress(request.getRemoteAddr());
            row.setUserAgent(request.getHeader("User-Agent"));
        }
        entityManager.persist(row);
    }

    public void logLogin(String userId, String username, String ip, String userAgent, boolean success) {
        AuditLog row = new AuditLog();
        if (userId != null && !userId.isBlank()) {
            row.setUser(entityManager.find(AppUser.class, userId));
        }
        row.setUsername(username);
        row.setAction(success ? "LOGIN" : "LOGIN_FAILED");
        row.setEntityType("Auth");
        row.setIpAddress(ip);
        row.setUserAgent(userAgent);
        entityManager.persist(row);
    }

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
}
