package bean;

import entity.CurrentAccount;
import enums.Permission;
import enums.Scope;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminCurrentAccountBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<CurrentAccount> accounts = Collections.emptyList();
    private CurrentAccount selected;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.ACCESS)) {
            accounts = Collections.emptyList();
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.READ)) {
            accounts = Collections.emptyList();
            return;
        }
        accounts = currentAccountFacade.findAll(false);
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.CREATE)) {
            return;
        }
        selected = new CurrentAccount();
        selected.setActive(Boolean.TRUE);
        selected.setType("CUSTOMER");
        selected.setCreditLimit(BigDecimal.ZERO);
        selected.setCurrentBalance(BigDecimal.ZERO);
    }

    public void openEdit(CurrentAccount row) {
        if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        CurrentAccount loaded = currentAccountFacade.findById(row.getId());
        selected = loaded != null ? loaded : row;
    }

    public void save() {
        if (selected == null) {
            return;
        }
        boolean isNew = selected.getId() == null || currentAccountFacade.findById(selected.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.CREATE)) {
                return;
            }
            if (selected.getCode() == null || selected.getCode().isBlank()) {
                selected.setCode(generateNextCode());
            }
        } else {
            if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.UPDATE)) {
                return;
            }
        }

        CurrentAccount existing = currentAccountFacade.findByCode(selected.getCode());
        if (existing != null && (selected.getId() == null || !existing.getId().equals(selected.getId()))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cari kodu", "Bu cari kodu zaten kullanılıyor."));
            return;
        }

        selected.setCode(selected.getCode().trim().toUpperCase(Locale.ROOT));
        selected.setType(selected.getType() == null ? "CUSTOMER" : selected.getType().trim().toUpperCase(Locale.ROOT));
        currentAccountFacade.save(selected);
        refresh();
        PrimeFaces.current().executeScript("PF('currentAccountDlg').hide()");
    }

    public void softDelete(CurrentAccount row) {
        if (!rbacProcedure.require(Scope.CURRENT_ACCOUNT, Permission.DELETE) || row == null || row.getId() == null) {
            return;
        }
        currentAccountFacade.softDelete(row.getId());
        refresh();
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public List<String> getTypeOptions() {
        return List.of("CUSTOMER", "SUPPLIER", "BOTH");
    }

    public List<CurrentAccount> getAccounts() {
        return accounts;
    }

    public CurrentAccount getSelected() {
        return selected;
    }

    public void setSelected(CurrentAccount selected) {
        this.selected = selected;
    }

    private String generateNextCode() {
        int max = 0;
        for (CurrentAccount account : currentAccountFacade.findAll(true)) {
            String code = account.getCode();
            if (code == null) {
                continue;
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            if (!normalized.startsWith("CA-")) {
                continue;
            }
            String digits = normalized.substring(3).replaceAll("\\D", "");
            if (digits.isEmpty()) {
                continue;
            }
            try {
                int parsed = Integer.parseInt(digits);
                if (parsed > max) {
                    max = parsed;
                }
            } catch (NumberFormatException ignored) {
                // skip non-standard old codes
            }
        }
        return String.format("CA-%04d", max + 1);
    }
}
