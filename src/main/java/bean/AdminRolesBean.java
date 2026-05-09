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
import java.util.List;

@Named
@ViewScoped
public class AdminRolesBean implements Serializable {

    @EJB
    private RoleFacadeLocal roleFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<Role> roles = Collections.emptyList();
    private Role editRole;
    private List<Permission> selectedPermissions = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.ROLE, Permission.READ)) {
            roles = Collections.emptyList();
            return;
        }
        roles = roleFacade.listByScope(Scope.ROLE);
    }

    public List<Permission> getPermissionOptions() {
        return Arrays.asList(Permission.values());
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
        editRole.setScope(Scope.ROLE);
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
