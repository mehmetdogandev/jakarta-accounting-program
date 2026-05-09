package bean;

import entity.BankAccount;
import entity.BankTransaction;
import entity.CurrentAccount;
import enums.Permission;
import enums.Scope;
import facadeLocal.BankAccountFacadeLocal;
import facadeLocal.CurrentAccountFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminBankAccountBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private BankAccountFacadeLocal bankAccountFacade;

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<BankAccount> accounts = Collections.emptyList();
    private List<BankTransaction> transactions = Collections.emptyList();
    private BankAccount selectedAccount;
    private BankTransaction newTransaction;
    private CurrentAccount transactionCurrentAccount;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.ACCESS)) {
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.READ)) {
            accounts = Collections.emptyList();
            transactions = Collections.emptyList();
            return;
        }
        accounts = bankAccountFacade.findAll();
        if (selectedAccount != null && selectedAccount.getId() != null) {
            BankAccount reloaded = bankAccountFacade.findById(selectedAccount.getId());
            selectedAccount = reloaded;
            transactions = reloaded == null ? List.of() : bankAccountFacade.listTransactions(reloaded.getId());
        }
    }

    public void openNewAccount() {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.CREATE)) {
            return;
        }
        selectedAccount = new BankAccount();
        selectedAccount.setCurrency("TRY");
        selectedAccount.setActive(Boolean.TRUE);
        selectedAccount.setBalance(BigDecimal.ZERO);
    }

    public void saveAccount() {
        if (selectedAccount == null) {
            return;
        }
        boolean isNew = selectedAccount.getId() == null || bankAccountFacade.findById(selectedAccount.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.UPDATE)) {
                return;
            }
        }
        BankAccount existing = bankAccountFacade.findByCode(selectedAccount.getCode());
        if (existing != null && (selectedAccount.getId() == null || !existing.getId().equals(selectedAccount.getId()))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Banka kodu", "Bu kod zaten kullanılıyor."));
            return;
        }
        bankAccountFacade.save(selectedAccount);
        refresh();
        PrimeFaces.current().executeScript("PF('bankAccountDlg').hide()");
    }

    public void selectAccount(BankAccount account) {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.READ) || account == null || account.getId() == null) {
            return;
        }
        selectedAccount = bankAccountFacade.findById(account.getId());
        transactions = selectedAccount == null ? List.of() : bankAccountFacade.listTransactions(selectedAccount.getId());
    }

    public void openNewTransaction(String type) {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.CREATE) || selectedAccount == null) {
            return;
        }
        newTransaction = new BankTransaction();
        newTransaction.setTransactionDate(LocalDate.now());
        newTransaction.setTransactionType(type == null ? "IN" : type);
        newTransaction.setAmount(BigDecimal.ZERO);
        transactionCurrentAccount = null;
    }

    public void saveTransaction() {
        if (!rbacProcedure.require(Scope.BANK_ACCOUNT, Permission.CREATE) || selectedAccount == null || newTransaction == null) {
            return;
        }
        bankAccountFacade.addTransaction(
                selectedAccount.getId(),
                newTransaction.getTransactionType(),
                newTransaction.getAmount(),
                newTransaction.getTransactionDate(),
                newTransaction.getDescription(),
                transactionCurrentAccount != null ? transactionCurrentAccount.getId() : null,
                newTransaction.getReferenceType(),
                newTransaction.getReferenceId(),
                rbacProcedure.currentUserId().orElse(null)
        );
        selectAccount(selectedAccount);
        PrimeFaces.current().executeScript("PF('bankTxnDlg').hide()");
    }

    public List<CurrentAccount> completeCurrentAccount(String query) {
        return currentAccountFacade.searchByName(query);
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public String transactionBadgeClass(String type) {
        return "IN".equalsIgnoreCase(type) ? "badge text-bg-success" : "badge text-bg-danger";
    }

    public List<BankAccount> getAccounts() {
        return accounts;
    }

    public List<BankTransaction> getTransactions() {
        return transactions;
    }

    public BankAccount getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(BankAccount selectedAccount) {
        this.selectedAccount = selectedAccount;
    }

    public BankTransaction getNewTransaction() {
        return newTransaction;
    }

    public void setNewTransaction(BankTransaction newTransaction) {
        this.newTransaction = newTransaction;
    }

    public CurrentAccount getTransactionCurrentAccount() {
        return transactionCurrentAccount;
    }

    public void setTransactionCurrentAccount(CurrentAccount transactionCurrentAccount) {
        this.transactionCurrentAccount = transactionCurrentAccount;
    }
}
