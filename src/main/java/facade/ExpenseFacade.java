package facade;

import entity.BankAccount;
import entity.CashAccount;
import entity.Expense;
import entity.ExpenseCategory;
import facadeLocal.ExpenseFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Stateless
public class ExpenseFacade extends AbstractFacade implements ExpenseFacadeLocal {

    @Override
    public List<Expense> findAll(boolean includeDeleted) {
        String jpql = "SELECT e FROM Expense e "
                + "LEFT JOIN FETCH e.category "
                + "LEFT JOIN FETCH e.currentAccount "
                + "LEFT JOIN FETCH e.cashAccount "
                + "LEFT JOIN FETCH e.bankAccount ";
        if (!includeDeleted) {
            jpql += "WHERE e.deletedAt IS NULL ";
        }
        jpql += "ORDER BY e.transactionDate DESC, e.createdAt DESC";
        return entityManager.createQuery(jpql, Expense.class).getResultList();
    }

    @Override
    public Expense findById(UUID id) {
        return entityManager.find(Expense.class, id);
    }

    @Override
    public List<Expense> findByType(String type) {
        String normalized = normalizeType(type);
        return entityManager.createQuery(
                        "SELECT e FROM Expense e "
                                + "LEFT JOIN FETCH e.category "
                                + "WHERE e.deletedAt IS NULL AND UPPER(e.transactionType) = :type "
                                + "ORDER BY e.transactionDate DESC",
                        Expense.class)
                .setParameter("type", normalized)
                .getResultList();
    }

    @Override
    public List<Expense> findByDateRange(LocalDate from, LocalDate to) {
        return entityManager.createQuery(
                        "SELECT e FROM Expense e "
                                + "LEFT JOIN FETCH e.category "
                                + "WHERE e.deletedAt IS NULL "
                                + "AND e.transactionDate BETWEEN :from AND :to "
                                + "ORDER BY e.transactionDate DESC",
                        Expense.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<Expense> findByStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return entityManager.createQuery(
                        "SELECT e FROM Expense e "
                                + "LEFT JOIN FETCH e.category "
                                + "WHERE e.deletedAt IS NULL AND UPPER(e.status) = :status "
                                + "ORDER BY e.transactionDate DESC",
                        Expense.class)
                .setParameter("status", normalized)
                .getResultList();
    }

    @Override
    public List<ExpenseCategory> findCategoriesByType(String type) {
        String normalized = normalizeType(type);
        return entityManager.createQuery(
                        "SELECT c FROM ExpenseCategory c WHERE UPPER(c.type) = :type ORDER BY c.name",
                        ExpenseCategory.class)
                .setParameter("type", normalized)
                .getResultList();
    }

    @Override
    public Expense save(Expense expense) {
        expense.setTransactionType(normalizeType(expense.getTransactionType()));
        normalizePayment(expense);
        if (expense.getId() == null || findById(expense.getId()) == null) {
            entityManager.persist(expense);
            entityManager.flush();
            return expense;
        }
        Expense merged = entityManager.merge(expense);
        entityManager.flush();
        return merged;
    }

    @Override
    public void approve(UUID id, String approvedByUserId) {
        Expense e = require(id);
        ensurePending(e);
        applyPaymentEffect(e);
        e.setStatus("APPROVED");
        e.setApprovedBy(approvedByUserId);
        e.setApprovedAt(Instant.now());
        entityManager.merge(e);
        entityManager.flush();
    }

    @Override
    public void reject(UUID id) {
        Expense e = require(id);
        ensurePending(e);
        e.setStatus("REJECTED");
        entityManager.merge(e);
        entityManager.flush();
    }

    @Override
    public void softDelete(UUID id) {
        Expense e = require(id);
        ensurePending(e);
        e.setDeletedAt(Instant.now());
        entityManager.merge(e);
        entityManager.flush();
    }

    @Override
    public Map<String, BigDecimal> getSummaryByCategory(LocalDate from, LocalDate to) {
        @SuppressWarnings("unchecked")
        List<Object> rawRows = entityManager.createNativeQuery(
                        "SELECT COALESCE(c.name, 'Kategori Yok') AS category_name, COALESCE(SUM(e.amount), 0) AS total_amount "
                                + "FROM expense e "
                                + "LEFT JOIN expense_category c ON c.id = e.category_id "
                                + "WHERE e.deleted_at IS NULL "
                                + "AND e.transaction_date BETWEEN :from AND :to "
                                + "GROUP BY category_name "
                                + "ORDER BY total_amount DESC")
                .setParameter("from", Date.valueOf(from))
                .setParameter("to", Date.valueOf(to))
                .getResultList();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object rowObj : rawRows) {
            Object[] row = (Object[]) rowObj;
            String key = row[0] == null ? "Kategori Yok" : row[0].toString();
            BigDecimal total = row[1] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            result.put(key, total);
        }
        return result;
    }

    private void applyPaymentEffect(Expense e) {
        String method = e.getPaymentMethod() == null ? "" : e.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        BigDecimal amount = e.getAmount() == null ? BigDecimal.ZERO : e.getAmount();
        boolean isExpense = "EXPENSE".equalsIgnoreCase(e.getTransactionType());

        if ("CASH".equals(method)) {
            if (e.getCashAccount() == null) {
                throw new IllegalStateException("Nakit ödeme için kasa hesabı seçilmelidir.");
            }
            CashAccount cash = entityManager.find(CashAccount.class, e.getCashAccount().getId());
            BigDecimal current = cash.getBalance() == null ? BigDecimal.ZERO : cash.getBalance();
            cash.setBalance(isExpense ? current.subtract(amount) : current.add(amount));
            entityManager.merge(cash);
        } else if ("BANK".equals(method) || "CREDIT_CARD".equals(method) || "CHEQUE".equals(method)) {
            if (e.getBankAccount() == null) {
                throw new IllegalStateException("Bu ödeme yöntemi için banka hesabı seçilmelidir.");
            }
            BankAccount bank = entityManager.find(BankAccount.class, e.getBankAccount().getId());
            BigDecimal current = bank.getBalance() == null ? BigDecimal.ZERO : bank.getBalance();
            bank.setBalance(isExpense ? current.subtract(amount) : current.add(amount));
            entityManager.merge(bank);
        }
    }

    private Expense require(UUID id) {
        Expense e = findById(id);
        if (e == null || e.getDeletedAt() != null) {
            throw new IllegalStateException("Kayıt bulunamadı.");
        }
        return e;
    }

    private static void ensurePending(Expense e) {
        if (!"PENDING".equalsIgnoreCase(e.getStatus())) {
            throw new IllegalStateException("Bu işlem sadece PENDING kayıtlar için yapılabilir.");
        }
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!"EXPENSE".equals(t) && !"INCOME".equals(t)) {
            throw new IllegalStateException("İşlem türü EXPENSE veya INCOME olmalıdır.");
        }
        return t;
    }

    private static void normalizePayment(Expense e) {
        if (e.getStatus() == null || e.getStatus().isBlank()) {
            e.setStatus("PENDING");
        } else {
            e.setStatus(e.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        if (e.getPaymentMethod() != null) {
            e.setPaymentMethod(e.getPaymentMethod().trim().toUpperCase(Locale.ROOT));
        }
        if (e.getTaxAmount() == null) {
            e.setTaxAmount(BigDecimal.ZERO);
        }
    }
}
