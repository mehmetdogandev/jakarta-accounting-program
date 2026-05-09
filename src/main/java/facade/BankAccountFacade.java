package facade;

import entity.BankAccount;
import entity.BankTransaction;
import entity.CurrentAccount;
import facadeLocal.BankAccountFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class BankAccountFacade extends AbstractFacade implements BankAccountFacadeLocal {

    @Override
    public List<BankAccount> findAll() {
        return entityManager.createQuery(
                        "SELECT b FROM BankAccount b ORDER BY b.code",
                        BankAccount.class)
                .getResultList();
    }

    @Override
    public BankAccount findById(UUID id) {
        return entityManager.find(BankAccount.class, id);
    }

    @Override
    public BankAccount findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        List<BankAccount> rows = entityManager.createQuery(
                        "SELECT b FROM BankAccount b WHERE LOWER(b.code) = :code",
                        BankAccount.class)
                .setParameter("code", code.trim().toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public List<BankAccount> findActive() {
        return entityManager.createQuery(
                        "SELECT b FROM BankAccount b WHERE b.active = true ORDER BY b.code",
                        BankAccount.class)
                .getResultList();
    }

    @Override
    public BankAccount save(BankAccount account) {
        if (account.getId() == null || findById(account.getId()) == null) {
            entityManager.persist(account);
            entityManager.flush();
            return account;
        }
        BankAccount merged = entityManager.merge(account);
        entityManager.flush();
        return merged;
    }

    @Override
    public BankTransaction addTransaction(UUID bankAccountId,
                                          String type,
                                          BigDecimal amount,
                                          LocalDate transactionDate,
                                          String description,
                                          UUID currentAccountId,
                                          String referenceType,
                                          UUID referenceId,
                                          String createdBy) {
        BankAccount account = findById(bankAccountId);
        if (account == null) {
            throw new IllegalStateException("Banka hesabı bulunamadı.");
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

        BankTransaction transaction = new BankTransaction();
        transaction.setBankAccount(account);
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
    public List<BankTransaction> listTransactions(UUID bankAccountId) {
        return entityManager.createQuery(
                        "SELECT t FROM BankTransaction t "
                                + "LEFT JOIN FETCH t.currentAccount "
                                + "WHERE t.bankAccount.id = :id "
                                + "ORDER BY t.transactionDate DESC, t.createdAt DESC",
                        BankTransaction.class)
                .setParameter("id", bankAccountId)
                .getResultList();
    }

    @Override
    public BigDecimal getBalance(UUID id) {
        BankAccount account = findById(id);
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
