package facadeLocal;

import entity.Expense;
import entity.ExpenseCategory;
import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Local
public interface ExpenseFacadeLocal {

    List<Expense> findAll(boolean includeDeleted);

    Expense findById(UUID id);

    List<Expense> findByType(String type);

    List<Expense> findByDateRange(LocalDate from, LocalDate to);

    List<Expense> findByStatus(String status);

    List<ExpenseCategory> findCategoriesByType(String type);

    Expense save(Expense expense);

    void approve(UUID id, String approvedByUserId);

    void reject(UUID id);

    void softDelete(UUID id);

    Map<String, java.math.BigDecimal> getSummaryByCategory(LocalDate from, LocalDate to);
}
