package facadeLocal;

import entity.JournalEntry;
import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Local
public interface JournalEntryFacadeLocal {

    List<JournalEntry> findAll(boolean includeCancelled);

    List<JournalEntry> findByDateRange(LocalDate from, LocalDate to);

    List<JournalEntry> findByStatus(String status);

    JournalEntry findById(UUID id);

    JournalEntry save(JournalEntry entry);

    void post(UUID entryId);

    void cancel(UUID entryId);

    String generateEntryNumber();

    void softDelete(UUID id);
}
