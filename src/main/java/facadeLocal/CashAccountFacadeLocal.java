package facadeLocal;

import entity.CashAccount;
import entity.CashTransaction;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Local
public interface CashAccountFacadeLocal {

    List<CashAccount> findAll();

    CashAccount findById(UUID id);

    CashAccount findByCode(String code);

    List<CashAccount> findActive();

    CashAccount save(CashAccount account);

    CashTransaction addTransaction(UUID cashAccountId,
                                   String type,
                                   BigDecimal amount,
                                   LocalDate transactionDate,
                                   String description,
                                   UUID currentAccountId,
                                   String referenceType,
                                   UUID referenceId,
                                   String createdBy);

    List<CashTransaction> listTransactions(UUID cashAccountId);

    BigDecimal getBalance(UUID id);
}
