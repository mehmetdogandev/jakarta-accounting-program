package facadeLocal;

import entity.AuditLog;
import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Local
public interface AuditLogFacadeLocal {
    List<AuditLog> findRecent(int limit);

    List<AuditLog> findByUser(String userId, LocalDate from, LocalDate to);

    List<AuditLog> findByEntityType(String entityType);

    long countByAction(String action, LocalDate from, LocalDate to);
}
