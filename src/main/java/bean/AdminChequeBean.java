package bean;

import entity.BankAccount;
import entity.Cheque;
import entity.CurrentAccount;
import enums.Permission;
import enums.Scope;
import facadeLocal.BankAccountFacadeLocal;
import facadeLocal.ChequeFacadeLocal;
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
public class AdminChequeBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private ChequeFacadeLocal chequeFacade;

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private BankAccountFacadeLocal bankAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<Cheque> cheques = Collections.emptyList();
    private Cheque selected;
    private String filterType = "";
    private String filterStatus = "";
    private CurrentAccount selectedCurrentAccount;
    private BankAccount selectedBankAccount;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.ACCESS)) {
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.READ)) {
            cheques = Collections.emptyList();
            return;
        }
        List<Cheque> base = chequeFacade.findAll();
        if (filterType != null && !filterType.isBlank()) {
            String t = filterType.trim().toUpperCase(Locale.ROOT);
            base = base.stream().filter(c -> t.equalsIgnoreCase(c.getChequeType())).toList();
        }
        if (filterStatus != null && !filterStatus.isBlank()) {
            String s = filterStatus.trim().toUpperCase(Locale.ROOT);
            base = base.stream().filter(c -> s.equalsIgnoreCase(c.getStatus())).toList();
        }
        cheques = base;
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.CREATE)) {
            return;
        }
        selected = new Cheque();
        selected.setChequeType("RECEIVED");
        selected.setIssueDate(LocalDate.now());
        selected.setDueDate(LocalDate.now().plusDays(30));
        selected.setAmount(BigDecimal.ZERO);
        selected.setStatus("PORTFOLIO");
        selectedCurrentAccount = null;
        selectedBankAccount = null;
    }

    public void save() {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.CREATE) || selected == null) {
            return;
        }
        if (selectedCurrentAccount == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cari", "Cari hesap zorunludur."));
            return;
        }
        selected.setCurrentAccount(selectedCurrentAccount);
        selected.setBankAccount(selectedBankAccount);
        selected.setCreatedBy(rbacProcedure.currentUserId().orElse(null));
        chequeFacade.save(selected);
        refresh();
        PrimeFaces.current().executeScript("PF('chequeDlg').hide()");
    }

    public void deposit(Cheque row) {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        if (selectedBankAccount == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Banka", "Önce bankaya yatırma için banka hesabı seçin."));
            return;
        }
        exec(() -> chequeFacade.deposit(row.getId(), selectedBankAccount.getId()));
    }

    public void collect(Cheque row) {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        exec(() -> chequeFacade.collect(row.getId()));
    }

    public void returnCheque(Cheque row) {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        exec(() -> chequeFacade.returnCheque(row.getId()));
    }

    public void protest(Cheque row) {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        exec(() -> chequeFacade.protest(row.getId()));
    }

    public void pay(Cheque row) {
        if (!rbacProcedure.require(Scope.CHEQUE, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        exec(() -> chequeFacade.pay(row.getId()));
    }

    public int getDueSoonCount() {
        return chequeFacade.findDueWithin(7).size();
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public boolean isOverdue(Cheque cheque) {
        return cheque != null && cheque.getDueDate() != null && cheque.getDueDate().isBefore(LocalDate.now());
    }

    public List<String> getTypeOptions() {
        return List.of("RECEIVED", "ISSUED");
    }

    public List<String> getStatusOptions() {
        return List.of("PORTFOLIO", "DEPOSITED", "COLLECTED", "RETURNED", "PROTESTED", "ISSUED", "PAID");
    }

    public List<CurrentAccount> completeCurrentAccount(String query) {
        return currentAccountFacade.searchByName(query);
    }

    public List<BankAccount> completeBankAccount(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return bankAccountFacade.findActive();
        }
        return bankAccountFacade.findActive().stream()
                .filter(b -> (b.getCode() != null && b.getCode().toLowerCase(Locale.ROOT).contains(q))
                        || (b.getBankName() != null && b.getBankName().toLowerCase(Locale.ROOT).contains(q)))
                .toList();
    }

    public String typeBadgeClass(String type) {
        return "RECEIVED".equalsIgnoreCase(type) ? "badge text-bg-primary" : "badge text-bg-warning";
    }

    public String statusBadgeClass(String status) {
        String s = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "PORTFOLIO", "ISSUED" -> "badge text-bg-secondary";
            case "DEPOSITED" -> "badge text-bg-info";
            case "COLLECTED", "PAID" -> "badge text-bg-success";
            case "RETURNED", "PROTESTED" -> "badge text-bg-danger";
            default -> "badge text-bg-secondary";
        };
    }

    public boolean canDeposit(Cheque c) {
        return c != null && "RECEIVED".equalsIgnoreCase(c.getChequeType()) && "PORTFOLIO".equalsIgnoreCase(c.getStatus());
    }

    public boolean canCollect(Cheque c) {
        return c != null && "RECEIVED".equalsIgnoreCase(c.getChequeType()) && "DEPOSITED".equalsIgnoreCase(c.getStatus());
    }

    public boolean canProtest(Cheque c) {
        return c != null && "RECEIVED".equalsIgnoreCase(c.getChequeType())
                && ("PORTFOLIO".equalsIgnoreCase(c.getStatus()) || "DEPOSITED".equalsIgnoreCase(c.getStatus()));
    }

    public boolean canPay(Cheque c) {
        return c != null && "ISSUED".equalsIgnoreCase(c.getChequeType()) && "ISSUED".equalsIgnoreCase(c.getStatus());
    }

    public boolean canReturn(Cheque c) {
        if (c == null) {
            return false;
        }
        if ("RECEIVED".equalsIgnoreCase(c.getChequeType())) {
            return "PORTFOLIO".equalsIgnoreCase(c.getStatus()) || "DEPOSITED".equalsIgnoreCase(c.getStatus());
        }
        return "ISSUED".equalsIgnoreCase(c.getChequeType()) && "ISSUED".equalsIgnoreCase(c.getStatus());
    }

    public List<Cheque> getCheques() {
        return cheques;
    }

    public Cheque getSelected() {
        return selected;
    }

    public void setSelected(Cheque selected) {
        this.selected = selected;
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

    public CurrentAccount getSelectedCurrentAccount() {
        return selectedCurrentAccount;
    }

    public void setSelectedCurrentAccount(CurrentAccount selectedCurrentAccount) {
        this.selectedCurrentAccount = selectedCurrentAccount;
    }

    public BankAccount getSelectedBankAccount() {
        return selectedBankAccount;
    }

    public void setSelectedBankAccount(BankAccount selectedBankAccount) {
        this.selectedBankAccount = selectedBankAccount;
    }

    private void exec(Runnable action) {
        try {
            action.run();
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Çek işlemi", ex.getMessage()));
        }
    }
}
