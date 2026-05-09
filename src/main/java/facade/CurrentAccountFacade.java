package facade;

import entity.CurrentAccount;
import facadeLocal.CurrentAccountFacadeLocal;
import jakarta.ejb.Stateless;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class CurrentAccountFacade extends AbstractFacade implements CurrentAccountFacadeLocal {

    @Override
    public List<CurrentAccount> findAll(boolean includeDeleted) {
        if (includeDeleted) {
            return entityManager.createQuery(
                            "SELECT c FROM CurrentAccount c ORDER BY c.createdAt DESC",
                            CurrentAccount.class)
                    .getResultList();
        }
        return entityManager.createQuery(
                        "SELECT c FROM CurrentAccount c WHERE c.deletedAt IS NULL ORDER BY c.createdAt DESC",
                        CurrentAccount.class)
                .getResultList();
    }

    @Override
    public CurrentAccount findById(UUID id) {
        return entityManager.find(CurrentAccount.class, id);
    }

    @Override
    public CurrentAccount findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<CurrentAccount> rows = entityManager.createQuery(
                        "SELECT c FROM CurrentAccount c "
                                + "WHERE c.deletedAt IS NULL AND LOWER(c.code) = :code",
                        CurrentAccount.class)
                .setParameter("code", code.trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<CurrentAccount> findByType(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if ("CUSTOMER".equals(normalized) || "SUPPLIER".equals(normalized)) {
            return entityManager.createQuery(
                            "SELECT c FROM CurrentAccount c "
                                    + "WHERE c.deletedAt IS NULL AND (UPPER(c.type) = :type OR UPPER(c.type) = 'BOTH') "
                                    + "ORDER BY c.name",
                            CurrentAccount.class)
                    .setParameter("type", normalized)
                    .getResultList();
        }
        return entityManager.createQuery(
                        "SELECT c FROM CurrentAccount c "
                                + "WHERE c.deletedAt IS NULL AND UPPER(c.type) = :type "
                                + "ORDER BY c.name",
                        CurrentAccount.class)
                .setParameter("type", normalized)
                .getResultList();
    }

    @Override
    public List<CurrentAccount> searchByName(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return findAll(false);
        }
        return entityManager.createQuery(
                        "SELECT c FROM CurrentAccount c "
                                + "WHERE c.deletedAt IS NULL AND LOWER(c.name) LIKE :q "
                                + "ORDER BY c.name",
                        CurrentAccount.class)
                .setParameter("q", "%" + q + "%")
                .getResultList();
    }

    @Override
    public CurrentAccount save(CurrentAccount account) {
        if (account.getId() == null || findById(account.getId()) == null) {
            entityManager.persist(account);
            entityManager.flush();
            return account;
        }
        CurrentAccount merged = entityManager.merge(account);
        entityManager.flush();
        return merged;
    }

    @Override
    public void softDelete(UUID id) {
        CurrentAccount currentAccount = findById(id);
        if (currentAccount == null || currentAccount.getDeletedAt() != null) {
            return;
        }
        currentAccount.setDeletedAt(Instant.now());
        entityManager.merge(currentAccount);
        entityManager.flush();
    }
}
