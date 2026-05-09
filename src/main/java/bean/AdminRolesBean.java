package bean;

import entity.Role;
import enums.Permission;
import enums.Scope;
import facadeLocal.RoleFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Named
@ViewScoped
public class AdminRolesBean implements Serializable {

    @EJB
    private RoleFacadeLocal roleFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<Role> roles = Collections.emptyList();
    private Map<UUID, List<Permission>> permissionsByRoleId = Collections.emptyMap();
    private Role editRole;
    private Role detailRole;
    private List<Permission> selectedPermissions = new ArrayList<>();
    private String roleSearch = "";

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.ROLE, Permission.READ)) {
            roles = Collections.emptyList();
            permissionsByRoleId = Collections.emptyMap();
            return;
        }
        List<Role> merged = new ArrayList<>();
        for (Scope scope : Scope.values()) {
            merged.addAll(roleFacade.listByScope(scope));
        }
        merged.sort(Comparator.comparing(Role::getScope).thenComparing(r -> r.getName(), String.CASE_INSENSITIVE_ORDER));
        roles = merged;

        Map<UUID, List<Permission>> map = new LinkedHashMap<>();
        for (Role r : roles) {
            map.put(r.getId(), List.copyOf(roleFacade.listPermissions(r.getId())));
        }
        permissionsByRoleId = map;
    }

    public List<Permission> getPermissionOptions() {
        return Arrays.asList(Permission.values());
    }

    public List<Scope> getScopeOptions() {
        return Arrays.asList(Scope.values());
    }

    public void openDetail(Role row) {
        if (!rbacProcedure.require(Scope.ROLE, Permission.READ)) {
            return;
        }
        Role loaded = roleFacade.findById(row.getId());
        this.detailRole = loaded != null ? loaded : row;
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.ROLE, Permission.CREATE)) {
            return;
        }
        editRole = new Role();
        editRole.setScope(Scope.ROLE);
        selectedPermissions = new ArrayList<>();
    }

    public void openEdit(Role row) {
        if (!rbacProcedure.require(Scope.ROLE, Permission.UPDATE)) {
            return;
        }
        Role loaded = roleFacade.findById(row.getId());
        this.editRole = loaded != null ? loaded : row;
        this.selectedPermissions = new ArrayList<>(roleFacade.listPermissions(editRole.getId()));
    }

    public void save() {
        if (editRole == null) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        boolean isNew = editRole.getId() == null || roleFacade.findById(editRole.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.ROLE, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.ROLE, Permission.UPDATE)) {
                return;
            }
        }
        roleFacade.save(editRole, selectedPermissions, actor);
        refresh();
        PrimeFaces.current().executeScript("PF('roleDlg').hide()");
    }

    public void softDelete(Role row) {
        if (!rbacProcedure.require(Scope.ROLE, Permission.DELETE)) {
            return;
        }
        roleFacade.softDelete(row.getId(), rbacProcedure.currentUserId().orElse(null));
        refresh();
    }

    public List<Role> getRoles() {
        return roles;
    }

    public String getRoleSearch() {
        return roleSearch;
    }

    public void setRoleSearch(String roleSearch) {
        this.roleSearch = roleSearch;
    }

    public List<Role> getFilteredRoles() {
        String q = roleSearch == null ? "" : roleSearch.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return roles;
        }
        return roles.stream()
                .filter(r -> r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public List<Permission> permissionsFor(Role r) {
        return permissionsByRoleId.getOrDefault(r.getId(), List.of());
    }

    public Role getDetailRole() {
        return detailRole;
    }

    public List<Permission> getDetailPermissions() {
        if (detailRole == null || detailRole.getId() == null) {
            return List.of();
        }
        return permissionsByRoleId.getOrDefault(detailRole.getId(), List.of());
    }

    public Role getEditRole() {
        return editRole;
    }

    public void setEditRole(Role editRole) {
        this.editRole = editRole;
    }

    public List<Permission> getSelectedPermissions() {
        return selectedPermissions;
    }

    public void setSelectedPermissions(List<Permission> selectedPermissions) {
        this.selectedPermissions = selectedPermissions;
    }
}
