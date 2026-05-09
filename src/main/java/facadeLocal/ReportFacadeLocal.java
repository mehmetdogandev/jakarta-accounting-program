package facadeLocal;

import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Local
public interface ReportFacadeLocal {

    List<Map<String, Object>> getTrialBalance(LocalDate asOf);

    Map<String, Object> getProfitLoss(LocalDate from, LocalDate to);

    Map<String, Object> getCashFlowSummary(LocalDate from, LocalDate to);

    List<Map<String, Object>> getReceivables();

    List<Map<String, Object>> getPayables();

    List<Map<String, Object>> getAgedReceivables();

    List<Map<String, Object>> getStockValuation();
}
