package facade;

import entity.CashAccount;
import entity.CashTransaction;
import entity.CurrentAccount;
import facadeLocal.CashAccountFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class CashAccountFacade extends AbstractFacade implements CashAccountFacadeLocal {

    @Override
    public List<CashAccount> findAll() {
        return entityManager.createQuery(
                        "SELECT c FROM CashAccount c ORDER BY c.code",
                        CashAccount.class)
                .getResultList();
    }

    @Override
    public CashAccount findById(UUID id) {
        return entityManager.find(CashAccount.class, id);
    }

    @Override
    public CashAccount findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<CashAccount> rows = entityManager.createQuery(
                        "SELECT c FROM CashAccount c WHERE LOWER(c.code) = :code",
                        CashAccount.class)
                .setParameter("code", code.trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<CashAccount> findActive() {
        return entityManager.createQuery(
                        "SELECT c FROM CashAccount c WHERE c.active = true ORDER BY c.code",
                        CashAccount.class)
                .getResultList();
    }

    @Override
    public CashAccount save(CashAccount account) {
        if (account.getId() == null || findById(account.getId()) == null) {
            entityManager.persist(account);
            entityManager.flush();
            return account;
        }
        CashAccount merged = entityManager.merge(account);
        entityManager.flush();
        return merged;
    }

    @Override
    public CashTransaction addTransaction(UUID cashAccountId,
                                          String type,
                                          BigDecimal amount,
                                          LocalDate transactionDate,
                                          String description,
                                          UUID currentAccountId,
                                          String referenceType,
                                          UUID referenceId,
                                          String createdBy) {
        CashAccount account = findById(cashAccountId);
        if (account == null) {
            throw new IllegalStateException("Kasa hesabı bulunamadı.");
        }
        String normalizedType = normalizeType(type);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Tutar sıfırdan büyük olmalıdır.");
        }

        BigDecimal currentBalance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        BigDecimal nextBalance = "IN".equals(normalizedType)
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);
        account.setBalance(nextBalance);
        entityManager.merge(account);

        CashTransaction transaction = new CashTransaction();
        transaction.setCashAccount(account);
        transaction.setTransactionType(normalizedType);
        transaction.setAmount(amount);
        transaction.setTransactionDate(transactionDate == null ? LocalDate.now() : transactionDate);
        transaction.setDescription(description);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setCreatedBy(createdBy);
        if (currentAccountId != null) {
            transaction.setCurrentAccount(entityManager.find(CurrentAccount.class, currentAccountId));
        }
        entityManager.persist(transaction);
        entityManager.flush();
        return transaction;
    }

    @Override
    public List<CashTransaction> listTransactions(UUID cashAccountId) {
        return entityManager.createQuery(
                        "SELECT t FROM CashTransaction t "
                                + "LEFT JOIN FETCH t.currentAccount "
                                + "WHERE t.cashAccount.id = :id "
                                + "ORDER BY t.transactionDate DESC, t.createdAt DESC",
                        CashTransaction.class)
                .setParameter("id", cashAccountId)
                .getResultList();
    }

    @Override
    public BigDecimal getBalance(UUID id) {
        CashAccount account = findById(id);
        return account == null || account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!"IN".equals(t) && !"OUT".equals(t)) {
            throw new IllegalStateException("İşlem tipi IN veya OUT olmalıdır.");
        }
        return t;
    }
}
