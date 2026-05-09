package bean;

import entity.AppUser;
import entity.AuditLog;
import enums.Permission;
import enums.Scope;
import facadeLocal.UserFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import procedure.RbacProcedureBean;
import service.AuditService;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Named
@ViewScoped
public class AdminAuditLogBean implements Serializable {

    @EJB
    private AuditService auditService;

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<AuditLog> logs = Collections.emptyList();
    private List<AppUser> users = Collections.emptyList();
    private String filterUserId = "";
    private String filterEntityType = "";
    private LocalDate filterFrom = LocalDate.now().minusDays(7);
    private LocalDate filterTo = LocalDate.now();
    private AuditLog selected;

    @PostConstruct
    public void init() {
        if (!rbacProcedure.require(Scope.AUDIT_LOG, Permission.ACCESS)) {
            return;
        }
        users = userFacade.usersList();
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.AUDIT_LOG, Permission.READ)) {
            logs = Collections.emptyList();
            return;
        }
        logs = auditService.findAll(
                filterFrom == null ? LocalDate.now().minusDays(7) : filterFrom,
                filterTo == null ? LocalDate.now() : filterTo,
                filterEntityType,
                filterUserId
        );
    }

    public String badgeClass(String action) {
        String a = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        return switch (a) {
            case "CREATE" -> "badge text-bg-primary";
            case "UPDATE" -> "badge text-bg-warning";
            case "DELETE" -> "badge text-bg-danger";
            case "LOGIN" -> "badge text-bg-success";
            case "APPROVE" -> "badge text-bg-info";
            default -> "badge text-bg-secondary";
        };
    }

    public String shortId(UUID id) {
        if (id == null) {
            return "-";
        }
        String s = id.toString();
        return s.substring(0, 8) + "...";
    }

    public List<AuditLog> getLogs() {
        return logs;
    }

    public List<AppUser> getUsers() {
        return users;
    }

    public String getFilterUserId() {
        return filterUserId;
    }

    public void setFilterUserId(String filterUserId) {
        this.filterUserId = filterUserId;
    }

    public String getFilterEntityType() {
        return filterEntityType;
    }

    public void setFilterEntityType(String filterEntityType) {
        this.filterEntityType = filterEntityType;
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

    public AuditLog getSelected() {
        return selected;
    }

    public void setSelected(AuditLog selected) {
        this.selected = selected;
    }
}
