package facade;

import entity.BankAccount;
import entity.Cheque;
import facadeLocal.ChequeFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class ChequeFacade extends AbstractFacade implements ChequeFacadeLocal {

    @Override
    public List<Cheque> findAll() {
        return entityManager.createQuery(
                        "SELECT c FROM Cheque c ORDER BY c.dueDate, c.createdAt DESC",
                        Cheque.class)
                .getResultList();
    }

    @Override
    public Cheque findById(UUID id) {
        return entityManager.find(Cheque.class, id);
    }

    @Override
    public List<Cheque> findByType(String type) {
        String normalized = normalizeType(type);
        return entityManager.createQuery(
                        "SELECT c FROM Cheque c WHERE UPPER(c.chequeType) = :type ORDER BY c.dueDate",
                        Cheque.class)
                .setParameter("type", normalized)
                .getResultList();
    }

    @Override
    public List<Cheque> findByStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return entityManager.createQuery(
                        "SELECT c FROM Cheque c WHERE UPPER(c.status) = :status ORDER BY c.dueDate",
                        Cheque.class)
                .setParameter("status", normalized)
                .getResultList();
    }

    @Override
    public List<Cheque> findDueWithin(int days) {
        LocalDate today = LocalDate.now();
        LocalDate target = today.plusDays(Math.max(days, 0));
        return entityManager.createQuery(
                        "SELECT c FROM Cheque c WHERE c.dueDate BETWEEN :today AND :target "
                                + "AND UPPER(c.status) NOT IN ('COLLECTED','PAID','RETURNED','PROTESTED') "
                                + "ORDER BY c.dueDate",
                        Cheque.class)
                .setParameter("today", today)
                .setParameter("target", target)
                .getResultList();
    }

    @Override
    public Cheque save(Cheque cheque) {
        cheque.setChequeType(normalizeType(cheque.getChequeType()));
        normalizeInitialStatus(cheque);
        if (cheque.getId() == null || findById(cheque.getId()) == null) {
            entityManager.persist(cheque);
            entityManager.flush();
            return cheque;
        }
        Cheque merged = entityManager.merge(cheque);
        entityManager.flush();
        return merged;
    }

    @Override
    public void deposit(UUID id, UUID bankAccountId) {
        Cheque cheque = require(id);
        ensureType(cheque, "RECEIVED");
        ensureStatus(cheque, "PORTFOLIO");
        BankAccount bank = entityManager.find(BankAccount.class, bankAccountId);
        if (bank == null) {
            throw new IllegalStateException("Banka hesabı bulunamadı.");
        }
        cheque.setBankAccount(bank);
        cheque.setStatus("DEPOSITED");
        entityManager.merge(cheque);
        entityManager.flush();
    }

    @Override
    public void collect(UUID id) {
        Cheque cheque = require(id);
        ensureType(cheque, "RECEIVED");
        ensureStatus(cheque, "DEPOSITED");
        if (cheque.getBankAccount() == null) {
            throw new IllegalStateException("Tahsil için bankaya yatırılmış çek gerekli.");
        }
        BankAccount bank = entityManager.find(BankAccount.class, cheque.getBankAccount().getId());
        BigDecimal current = bank.getBalance() == null ? BigDecimal.ZERO : bank.getBalance();
        bank.setBalance(current.add(cheque.getAmount()));
        cheque.setStatus("COLLECTED");
        entityManager.merge(bank);
        entityManager.merge(cheque);
        entityManager.flush();
    }

    @Override
    public void returnCheque(UUID id) {
        Cheque cheque = require(id);
        if ("RECEIVED".equalsIgnoreCase(cheque.getChequeType())) {
            if (!List.of("PORTFOLIO", "DEPOSITED").contains(cheque.getStatus().toUpperCase(Locale.ROOT))) {
                throw new IllegalStateException("Alınan çek sadece PORTFOLIO/DEPOSITED durumundan iade edilebilir.");
            }
            cheque.setStatus("RETURNED");
        } else if ("ISSUED".equalsIgnoreCase(cheque.getChequeType())) {
            ensureStatus(cheque, "ISSUED");
            cheque.setStatus("RETURNED");
        } else {
            throw new IllegalStateException("Geçersiz çek tipi.");
        }
        entityManager.merge(cheque);
        entityManager.flush();
    }

    @Override
    public void protest(UUID id) {
        Cheque cheque = require(id);
        ensureType(cheque, "RECEIVED");
        if (!List.of("PORTFOLIO", "DEPOSITED").contains(cheque.getStatus().toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException("Sadece portföydeki veya yatırılan çek protesto edilebilir.");
        }
        cheque.setStatus("PROTESTED");
        entityManager.merge(cheque);
        entityManager.flush();
    }

    @Override
    public void pay(UUID id) {
        Cheque cheque = require(id);
        ensureType(cheque, "ISSUED");
        ensureStatus(cheque, "ISSUED");
        if (cheque.getBankAccount() == null) {
            throw new IllegalStateException("Ödeme için banka hesabı zorunludur.");
        }
        BankAccount bank = entityManager.find(BankAccount.class, cheque.getBankAccount().getId());
        BigDecimal current = bank.getBalance() == null ? BigDecimal.ZERO : bank.getBalance();
        bank.setBalance(current.subtract(cheque.getAmount()));
        cheque.setStatus("PAID");
        entityManager.merge(bank);
        entityManager.merge(cheque);
        entityManager.flush();
    }

    private Cheque require(UUID id) {
        Cheque cheque = findById(id);
        if (cheque == null) {
            throw new IllegalStateException("Çek bulunamadı.");
        }
        return cheque;
    }

    private static void ensureStatus(Cheque cheque, String expected) {
        if (!expected.equalsIgnoreCase(cheque.getStatus())) {
            throw new IllegalStateException("Geçersiz durum geçişi.");
        }
    }

    private static void ensureType(Cheque cheque, String expected) {
        if (!expected.equalsIgnoreCase(cheque.getChequeType())) {
            throw new IllegalStateException("Bu işlem çek tipiyle uyumlu değil.");
        }
    }

    private static String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!"RECEIVED".equals(t) && !"ISSUED".equals(t)) {
            throw new IllegalStateException("Çek tipi RECEIVED veya ISSUED olmalıdır.");
        }
        return t;
    }

    private static void normalizeInitialStatus(Cheque cheque) {
        String status = cheque.getStatus();
        if (status == null || status.isBlank()) {
            cheque.setStatus("RECEIVED".equalsIgnoreCase(cheque.getChequeType()) ? "PORTFOLIO" : "ISSUED");
            return;
        }
        cheque.setStatus(status.trim().toUpperCase(Locale.ROOT));
    }
}
