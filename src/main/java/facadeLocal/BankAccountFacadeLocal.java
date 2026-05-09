package facadeLocal;

import entity.BankAccount;
import entity.BankTransaction;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Local
public interface BankAccountFacadeLocal {

    List<BankAccount> findAll();

    BankAccount findById(UUID id);

    BankAccount findByCode(String code);

    List<BankAccount> findActive();

    BankAccount save(BankAccount account);

    BankTransaction addTransaction(UUID bankAccountId,
                                   String type,
                                   BigDecimal amount,
                                   LocalDate transactionDate,
                                   String description,
                                   UUID currentAccountId,
                                   String referenceType,
                                   UUID referenceId,
                                   String createdBy);

    List<BankTransaction> listTransactions(UUID bankAccountId);

    BigDecimal getBalance(UUID id);
}
