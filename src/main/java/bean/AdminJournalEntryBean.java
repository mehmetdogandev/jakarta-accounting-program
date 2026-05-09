package bean;

import entity.CurrentAccount;
import entity.JournalEntry;
import entity.JournalEntryLine;
import enums.Permission;
import enums.Scope;
import facadeLocal.CurrentAccountFacadeLocal;
import facadeLocal.JournalEntryFacadeLocal;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Named
@ViewScoped
public class AdminJournalEntryBean implements Serializable {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    @EJB
    private JournalEntryFacadeLocal journalEntryFacade;

    @EJB
    private CurrentAccountFacadeLocal currentAccountFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    @EJB
    private AuditService auditService;

    private List<JournalEntry> entries = Collections.emptyList();
    private JournalEntry selected;
    private List<JournalEntryLine> editLines = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.ACCESS)) {
            entries = Collections.emptyList();
            return;
        }
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.READ)) {
            entries = Collections.emptyList();
            return;
        }
        entries = journalEntryFacade.findAll(true);
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.CREATE)) {
            return;
        }
        selected = new JournalEntry();
        selected.setEntryDate(LocalDate.now());
        selected.setStatus("DRAFT");
        selected.setEntryNumber(journalEntryFacade.generateEntryNumber());
        selected.setDescription("");
        editLines = new ArrayList<>();
        addLine();
    }

    public void openEdit(JournalEntry row) {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        JournalEntry loaded = journalEntryFacade.findById(row.getId());
        if (loaded == null) {
            return;
        }
        if (!"DRAFT".equalsIgnoreCase(loaded.getStatus())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Fiş", "Sadece DRAFT fiş düzenlenebilir."));
            return;
        }
        selected = loaded;
        editLines = new ArrayList<>(loaded.getLines() == null ? List.of() : loaded.getLines());
        editLines.sort(Comparator.comparingInt(JournalEntryLine::getLineOrder));
        if (editLines.isEmpty()) {
            addLine();
        }
    }

    public void addLine() {
        if (!requireCreateOrUpdate()) {
            return;
        }
        JournalEntryLine line = new JournalEntryLine();
        line.setLineOrder(editLines.size() + 1);
        line.setAccountCode("");
        line.setAccountName("");
        line.setDebit(BigDecimal.ZERO);
        line.setCredit(BigDecimal.ZERO);
        editLines.add(line);
    }

    public void removeLine(int index) {
        if (!requireCreateOrUpdate()) {
            return;
        }
        if (index < 0 || index >= editLines.size()) {
            return;
        }
        editLines.remove(index);
        reindexLines();
    }

    public void save() {
        if (selected == null) {
            return;
        }
        boolean isNew = selected.getId() == null || journalEntryFacade.findById(selected.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.UPDATE)) {
                return;
            }
            if (!"DRAFT".equalsIgnoreCase(selected.getStatus())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fiş", "POSTED/CANCELLED fiş kaydedilemez."));
                return;
            }
        }
        if (editLines.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Satırlar", "En az bir satır girilmelidir."));
            return;
        }
        if (getTotalDebit().compareTo(getTotalCredit()) != 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Denge", "Toplam borç ve alacak eşit olmalıdır."));
            return;
        }

        if (selected.getEntryNumber() == null || selected.getEntryNumber().isBlank()) {
            selected.setEntryNumber(journalEntryFacade.generateEntryNumber());
        }
        if (selected.getEntryDate() == null) {
            selected.setEntryDate(LocalDate.now());
        }
        if (selected.getStatus() == null || selected.getStatus().isBlank()) {
            selected.setStatus("DRAFT");
        }
        selected.setCreatedBy(rbacProcedure.currentUserId().orElse(null));
        selected.setTotalDebit(getTotalDebit());
        selected.setTotalCredit(getTotalCredit());

        List<JournalEntryLine> persistedLines = new ArrayList<>();
        for (int i = 0; i < editLines.size(); i++) {
            JournalEntryLine line = editLines.get(i);
            line.setLineOrder(i + 1);
            line.setJournalEntry(selected);
            if (line.getDebit() == null) {
                line.setDebit(BigDecimal.ZERO);
            }
            if (line.getCredit() == null) {
                line.setCredit(BigDecimal.ZERO);
            }
            persistedLines.add(line);
        }
        selected.setLines(persistedLines);
        journalEntryFacade.save(selected);
        auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), isNew ? "CREATE" : "UPDATE", selected);
        refresh();
        PrimeFaces.current().executeScript("PF('journalEntryDlg').hide()");
    }

    public void post(JournalEntry row) {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        try {
            journalEntryFacade.post(row.getId());
            auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "APPROVE", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fiş post", ex.getMessage()));
        }
    }

    public void cancel(JournalEntry row) {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.UPDATE) || row == null || row.getId() == null) {
            return;
        }
        try {
            journalEntryFacade.cancel(row.getId());
            auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "CANCEL", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fiş iptal", ex.getMessage()));
        }
    }

    public void softDelete(JournalEntry row) {
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.DELETE) || row == null || row.getId() == null) {
            return;
        }
        try {
            journalEntryFacade.softDelete(row.getId());
            auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "DELETE", row);
            refresh();
        } catch (IllegalStateException ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fiş sil", ex.getMessage()));
        }
    }

    public BigDecimal getTotalDebit() {
        return editLines.stream()
                .map(line -> line.getDebit() == null ? BigDecimal.ZERO : line.getDebit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCredit() {
        return editLines.stream()
                .map(line -> line.getCredit() == null ? BigDecimal.ZERO : line.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isSelectedPosted() {
        return selected != null && "POSTED".equalsIgnoreCase(selected.getStatus());
    }

    public boolean isBalanced() {
        return getTotalDebit().compareTo(getTotalCredit()) == 0;
    }

    public String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getCurrencyInstance(TURKISH).format(safe);
    }

    public String statusClass(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "POSTED" -> "badge text-bg-success";
            case "CANCELLED" -> "badge text-bg-danger";
            default -> "badge text-bg-secondary";
        };
    }

    public List<CurrentAccount> completeCurrentAccount(String query) {
        return currentAccountFacade.searchByName(query);
    }

    public List<JournalEntry> getEntries() {
        return entries;
    }

    public JournalEntry getSelected() {
        return selected;
    }

    public void setSelected(JournalEntry selected) {
        this.selected = selected;
    }

    public List<JournalEntryLine> getEditLines() {
        return editLines;
    }

    public void setEditLines(List<JournalEntryLine> editLines) {
        this.editLines = editLines;
    }

    private boolean requireCreateOrUpdate() {
        boolean isNew = selected == null || selected.getId() == null;
        if (isNew) {
            return rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.CREATE);
        }
        if (!rbacProcedure.require(Scope.JOURNAL_ENTRY, Permission.UPDATE)) {
            return false;
        }
        return "DRAFT".equalsIgnoreCase(selected.getStatus());
    }

    private void reindexLines() {
        for (int i = 0; i < editLines.size(); i++) {
            editLines.get(i).setLineOrder(i + 1);
        }
    }

    private String currentUsername() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return u instanceof entity.AppUser au ? au.getEmail() : null;
    }
}
