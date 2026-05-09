package facade;

import entity.JournalEntry;
import entity.JournalEntryLine;
import facadeLocal.JournalEntryFacadeLocal;
import jakarta.ejb.Stateless;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Stateless
public class JournalEntryFacade extends AbstractFacade implements JournalEntryFacadeLocal {

    @Override
    public List<JournalEntry> findAll(boolean includeCancelled) {
        if (includeCancelled) {
            return entityManager.createQuery(
                            "SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l "
                                    + "WHERE j.deletedAt IS NULL ORDER BY j.entryDate DESC, j.createdAt DESC",
                            JournalEntry.class)
                    .getResultList();
        }
        return entityManager.createQuery(
                        "SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l "
                                + "WHERE j.deletedAt IS NULL AND UPPER(j.status) <> 'CANCELLED' "
                                + "ORDER BY j.entryDate DESC, j.createdAt DESC",
                        JournalEntry.class)
                .getResultList();
    }

    @Override
    public List<JournalEntry> findByDateRange(LocalDate from, LocalDate to) {
        return entityManager.createQuery(
                        "SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l "
                                + "WHERE j.deletedAt IS NULL AND j.entryDate BETWEEN :from AND :to "
                                + "ORDER BY j.entryDate DESC, j.createdAt DESC",
                        JournalEntry.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<JournalEntry> findByStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return entityManager.createQuery(
                        "SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l "
                                + "WHERE j.deletedAt IS NULL AND UPPER(j.status) = :status "
                                + "ORDER BY j.entryDate DESC, j.createdAt DESC",
                        JournalEntry.class)
                .setParameter("status", normalized)
                .getResultList();
    }

    @Override
    public JournalEntry findById(UUID id) {
        if (id == null) {
            return null;
        }
        List<JournalEntry> rows = entityManager.createQuery(
                        "SELECT DISTINCT j FROM JournalEntry j LEFT JOIN FETCH j.lines l "
                                + "WHERE j.id = :id",
                        JournalEntry.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return null;
        }
        JournalEntry loaded = rows.get(0);
        loaded.getLines().sort(Comparator.comparingInt(JournalEntryLine::getLineOrder));
        return loaded;
    }

    @Override
    public JournalEntry save(JournalEntry entry) {
        recalculateTotals(entry);
        if (entry.getStatus() == null || entry.getStatus().isBlank()) {
            entry.setStatus("DRAFT");
        }
        if (entry.getId() == null || entityManager.find(JournalEntry.class, entry.getId()) == null) {
            entityManager.persist(entry);
            entityManager.flush();
            return entry;
        }
        JournalEntry merged = entityManager.merge(entry);
        entityManager.flush();
        return merged;
    }

    @Override
    public void post(UUID entryId) {
        JournalEntry entry = findById(entryId);
        if (entry == null || entry.getDeletedAt() != null) {
            throw new IllegalStateException("Fiş bulunamadı.");
        }
        recalculateTotals(entry);
        if (!entry.isBalanced()) {
            throw new IllegalStateException("Fiş dengede değil. Borç ve alacak eşit olmalı.");
        }
        entry.setStatus("POSTED");
        entityManager.merge(entry);
        entityManager.flush();
    }

    @Override
    public void cancel(UUID entryId) {
        JournalEntry entry = findById(entryId);
        if (entry == null || entry.getDeletedAt() != null) {
            throw new IllegalStateException("Fiş bulunamadı.");
        }
        if ("POSTED".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalStateException("POSTED fiş iptal edilemez.");
        }
        entry.setStatus("CANCELLED");
        entityManager.merge(entry);
        entityManager.flush();
    }

    @Override
    public String generateEntryNumber() {
        int year = LocalDate.now().getYear();
        String prefix = "FIS-" + year + "-";
        List<String> rows = entityManager.createQuery(
                        "SELECT j.entryNumber FROM JournalEntry j WHERE j.entryNumber LIKE :prefix",
                        String.class)
                .setParameter("prefix", prefix + "%")
                .getResultList();
        int max = 0;
        for (String row : rows) {
            if (row == null || !row.startsWith(prefix)) {
                continue;
            }
            String suffix = row.substring(prefix.length()).replaceAll("\\D", "");
            if (suffix.isEmpty()) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // ignore malformed numbers
            }
        }
        return String.format("FIS-%d-%04d", year, max + 1);
    }

    @Override
    public void softDelete(UUID id) {
        JournalEntry entry = findById(id);
        if (entry == null || entry.getDeletedAt() != null) {
            return;
        }
        if (!"DRAFT".equalsIgnoreCase(entry.getStatus())) {
            throw new IllegalStateException("Sadece DRAFT fişler silinebilir.");
        }
        entry.setDeletedAt(Instant.now());
        entityManager.merge(entry);
        entityManager.flush();
    }

    private static void recalculateTotals(JournalEntry entry) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        if (entry.getLines() != null) {
            for (JournalEntryLine line : entry.getLines()) {
                BigDecimal debit = line.getDebit() == null ? BigDecimal.ZERO : line.getDebit();
                BigDecimal credit = line.getCredit() == null ? BigDecimal.ZERO : line.getCredit();
                totalDebit = totalDebit.add(debit);
                totalCredit = totalCredit.add(credit);
            }
        }
        entry.setTotalDebit(totalDebit);
        entry.setTotalCredit(totalCredit);
    }
}
