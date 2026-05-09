package bean;

import entity.BankAccount;
import entity.CashAccount;
import entity.CurrentAccount;
import entity.Expense;
import entity.ExpenseCategory;
import enums.Permission;
import enums.Scope;
import facadeLocal.BankAccountFacadeLocal;
import facadeLocal.CashAccountFacadeLocal;
import facadeLocal.CurrentAccountFacadeLocal;
import facadeLocal.ExpenseFacadeLocal;
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
import java.util.UUID;

@Named
@ViewScoped
public class AdminExpenseBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private ExpenseFacadeLocal expenseFacade;

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private CashAccountFacadeLocal cashAccountFacade;

    @EJB
    private BankAccountFacadeLocal bankAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    @EJB
    private AuditService auditService;

    private List<Expense> records = Collections.emptyList();
    private List<ExpenseCategory> categories = Collections.emptyList();
    private String filterType = "EXPENSE";
    private String filterStatus = "";
    private LocalDate filterFrom = LocalDate.now().withDayOfMonth(1);
    private LocalDate filterTo = LocalDate.now();
    private Expense selected;
    private int tabIndex = 0;
    private String selectedCategoryId;
    private CurrentAccount selectedCurrentAccount;
    private String selectedCashAccountId;
    private String selectedBankAccountId;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.ACCESS)) {
            return;
        }
        refresh();
    }

    public void onTabChange() {
        filterType = tabIndex == 0 ? "EXPENSE" : "INCOME";
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.READ)) {
            records = Collections.emptyList();
            categories = Collections.emptyList();
            return;
        }
        categories = expenseFacade.findCategoriesByType(filterType);
        List<Expense> base = expenseFacade.findAll(false).stream()
                .filter(e -> filterType.equalsIgnoreCase(e.getTransactionType()))
                .toList();
        if (filterStatus != null && !filterStatus.isBlank()) {
            String st = filterStatus.trim().toUpperCase(Locale.ROOT);
            base = base.stream().filter(e -> st.equalsIgnoreCase(e.getStatus())).toList();
        }
        if (filterFrom != null && filterTo != null) {
            LocalDate from = filterFrom;
            LocalDate to = filterTo;
            base = base.stream()
                    .filter(e -> e.getTransactionDate() != null
                            && !e.getTransactionDate().isBefore(from)
                            && !e.getTransactionDate().isAfter(to))
                    .toList();
        }
        records = base;
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.CREATE)) {
            return;
        }
        selected = new Expense();
        selected.setTransactionType(filterType);
        selected.setTransactionDate(LocalDate.now());
        selected.setAmount(BigDecimal.ZERO);
        selected.setTaxAmount(BigDecimal.ZERO);
        selected.setStatus("PENDING");
        selectedCategoryId = null;
        selectedCurrentAccount = null;
        selectedCashAccountId = null;
        selectedBankAccountId = null;
    }

    public void save() {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.CREATE) || selected == null) {
            return;
        }
        selected.setTransactionType(filterType);
        selected.setCategory(parseUuid(selectedCategoryId) == null ? null : entityByCategory(parseUuid(selectedCategoryId)));
        selected.setCurrentAccount(selectedCurrentAccount);
        selected.setCashAccount(parseUuid(selectedCashAccountId) == null
                ? null : cashAccountFacade.findById(parseUuid(selectedCashAccountId)));
        selected.setBankAccount(parseUuid(selectedBankAccountId) == null
                ? null : bankAccountFacade.findById(parseUuid(selectedBankAccountId)));
        String actor = rbacProcedure.currentUserId().orElse(null);
        selected.setCreatedBy(actor);

        try {
            expenseFacade.save(selected);
            auditService.logAction(actor, currentUsername(), "CREATE", selected);
            refresh();
            PrimeFaces.current().executeScript("PF('expenseDlg').hide()");
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt", ex.getMessage()));
        }
    }

    public void approve(Expense row) {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        try {
            String actor = rbacProcedure.currentUserId().orElse(null);
            expenseFacade.approve(row.getId(), actor);
            auditService.logAction(actor, currentUsername(), "APPROVE", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Onay", ex.getMessage()));
        }
    }

    public void reject(Expense row) {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        try {
            expenseFacade.reject(row.getId());
            auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "REJECT", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Red", ex.getMessage()));
        }
    }

    public void softDelete(Expense row) {
        if (!rbacProcedure.require(Scope.EXPENSE, Permission.DELETE) || row == null || row.getId() == null) {
            return;
        }
        try {
            expenseFacade.softDelete(row.getId());
            auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "DELETE", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Sil", ex.getMessage()));
        }
    }

    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : records) {
            total = total.add(e.getAmount() == null ? BigDecimal.ZERO : e.getAmount());
        }
        return total;
    }

    public long getPendingCount() {
        return records.stream().filter(e -> "PENDING".equalsIgnoreCase(e.getStatus())).count();
    }

    public String formatCurrency(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(TURKISH).format(amount == null ? BigDecimal.ZERO : amount);
    }

    public String statusBadgeClass(String status) {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "APPROVED" -> "badge text-bg-success";
            case "REJECTED" -> "badge text-bg-danger";
            default -> "badge text-bg-secondary";
        };
    }

    public List<String> getStatusOptions() {
        return List.of("PENDING", "APPROVED", "REJECTED");
    }

    public List<String> getPaymentMethodOptions() {
        return List.of("CASH", "BANK", "CHEQUE", "CREDIT_CARD");
    }

    public List<CurrentAccount> completeCurrentAccount(String q) {
        return currentAccountFacade.searchByName(q);
    }

    public List<Expense> getRecords() {
        return records;
    }

    public List<ExpenseCategory> getCategories() {
        return categories;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public String getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
    }

    public LocalDate getFilterFrom() {
        return filterFrom;
    }

    public void setFilterFrom(LocalDate filterFrom) {
        this.filterFrom = filterFrom;
    }

    public LocalDate getFilterTo() {
        return filterTo;
    }

    public void setFilterTo(LocalDate filterTo) {
        this.filterTo = filterTo;
    }

    public Expense getSelected() {
        return selected;
    }

    public void setSelected(Expense selected) {
        this.selected = selected;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public String getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(String selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }

    public CurrentAccount getSelectedCurrentAccount() {
        return selectedCurrentAccount;
    }

    public void setSelectedCurrentAccount(CurrentAccount selectedCurrentAccount) {
        this.selectedCurrentAccount = selectedCurrentAccount;
    }

    public String getSelectedCashAccountId() {
        return selectedCashAccountId;
    }

    public void setSelectedCashAccountId(String selectedCashAccountId) {
        this.selectedCashAccountId = selectedCashAccountId;
    }

    public String getSelectedBankAccountId() {
        return selectedBankAccountId;
    }

    public void setSelectedBankAccountId(String selectedBankAccountId) {
        this.selectedBankAccountId = selectedBankAccountId;
    }

    public List<CashAccount> getCashAccounts() {
        return cashAccountFacade.findActive();
    }

    public List<BankAccount> getBankAccounts() {
        return bankAccountFacade.findActive();
    }

    private ExpenseCategory entityByCategory(UUID id) {
        return categories.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String currentUsername() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return u instanceof entity.AppUser au ? au.getEmail() : null;
    }
}
