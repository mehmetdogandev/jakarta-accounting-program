package bean;

import entity.CashAccount;
import entity.CashTransaction;
import entity.CurrentAccount;
import enums.Permission;
import enums.Scope;
import facadeLocal.CashAccountFacadeLocal;
import facadeLocal.CurrentAccountFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;
import service.AuditService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminCashAccountBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private CashAccountFacadeLocal cashAccountFacade;

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    @EJB
    private AuditService auditService;

    private List<CashAccount> accounts = Collections.emptyList();
    private List<CashTransaction> transactions = Collections.emptyList();
    private CashAccount selectedAccount;
    private CashTransaction newTransaction;
    private CurrentAccount transactionCurrentAccount;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.ACCESS)) {
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.READ)) {
            accounts = Collections.emptyList();
            transactions = Collections.emptyList();
            return;
        }
        accounts = cashAccountFacade.findAll();
        if (selectedAccount != null && selectedAccount.getId() != null) {
            CashAccount reloaded = cashAccountFacade.findById(selectedAccount.getId());
            selectedAccount = reloaded;
            transactions = reloaded == null ? List.of() : cashAccountFacade.listTransactions(reloaded.getId());
        }
    }

    public void openNewAccount() {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.CREATE)) {
            return;
        }
        selectedAccount = new CashAccount();
        selectedAccount.setCurrency("TRY");
        selectedAccount.setActive(Boolean.TRUE);
        selectedAccount.setBalance(BigDecimal.ZERO);
    }

    public void saveAccount() {
        if (selectedAccount == null) {
            return;
        }
        boolean isNew = selectedAccount.getId() == null || cashAccountFacade.findById(selectedAccount.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.UPDATE)) {
                return;
            }
        }
        CashAccount existing = cashAccountFacade.findByCode(selectedAccount.getCode());
        if (existing != null && (selectedAccount.getId() == null || !existing.getId().equals(selectedAccount.getId()))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kasa kodu", "Bu kod zaten kullanılıyor."));
            return;
        }
        cashAccountFacade.save(selectedAccount);
        auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), isNew ? "CREATE" : "UPDATE", selectedAccount);
        refresh();
        PrimeFaces.current().executeScript("PF('cashAccountDlg').hide()");
    }

    public void selectAccount(CashAccount account) {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.READ) || account == null || account.getId() == null) {
            return;
        }
        selectedAccount = cashAccountFacade.findById(account.getId());
        transactions = selectedAccount == null ? List.of() : cashAccountFacade.listTransactions(selectedAccount.getId());
    }

    public void openNewTransaction(String type) {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.CREATE) || selectedAccount == null) {
            return;
        }
        newTransaction = new CashTransaction();
        newTransaction.setTransactionDate(LocalDate.now());
        newTransaction.setTransactionType(type == null ? "IN" : type);
        newTransaction.setAmount(BigDecimal.ZERO);
        transactionCurrentAccount = null;
    }

    public void saveTransaction() {
        if (!rbacProcedure.require(Scope.CASH_ACCOUNT, Permission.CREATE) || selectedAccount == null || newTransaction == null) {
            return;
        }
        cashAccountFacade.addTransaction(
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
        auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "CREATE", newTransaction);
        selectAccount(selectedAccount);
        PrimeFaces.current().executeScript("PF('cashTxnDlg').hide()");
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

    public List<CashAccount> getAccounts() {
        return accounts;
    }

    public List<CashTransaction> getTransactions() {
        return transactions;
    }

    public CashAccount getSelectedAccount() {
        return selectedAccount;
    }

    public void setSelectedAccount(CashAccount selectedAccount) {
        this.selectedAccount = selectedAccount;
    }

    public CashTransaction getNewTransaction() {
        return newTransaction;
    }

    public void setNewTransaction(CashTransaction newTransaction) {
        this.newTransaction = newTransaction;
    }

    public CurrentAccount getTransactionCurrentAccount() {
        return transactionCurrentAccount;
    }

    public void setTransactionCurrentAccount(CurrentAccount transactionCurrentAccount) {
        this.transactionCurrentAccount = transactionCurrentAccount;
    }

    private String currentUsername() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return u instanceof entity.AppUser au ? au.getEmail() : null;
    }
}
