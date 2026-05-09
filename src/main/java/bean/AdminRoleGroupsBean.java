package bean;

import entity.Role;
import entity.RoleGroup;
import enums.Permission;
import enums.Scope;
import facadeLocal.RoleFacadeLocal;
import facadeLocal.RoleGroupFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Named
@ViewScoped
public class AdminRoleGroupsBean implements Serializable {

    @EJB
    private RoleGroupFacadeLocal roleGroupFacade;

    @EJB
    private RoleFacadeLocal roleFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<RoleGroup> groups = Collections.emptyList();
    private List<Role> assignableRoles = Collections.emptyList();
    private RoleGroup editGroup;
    private List<Role> selectedRoles = new ArrayList<>();

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.READ)) {
            groups = Collections.emptyList();
            assignableRoles = Collections.emptyList();
            return;
        }
        groups = roleGroupFacade.listActive();
        assignableRoles = roleGroupFacade.listRolesForAssignment();
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.CREATE)) {
            return;
        }
        editGroup = new RoleGroup();
        selectedRoles = new ArrayList<>();
    }

    public void openEdit(RoleGroup row) {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.UPDATE)) {
            return;
        }
        RoleGroup loaded = roleGroupFacade.findById(row.getId());
        this.editGroup = loaded != null ? loaded : row;
        selectedRoles = new ArrayList<>();
        for (UUID id : roleGroupFacade.linkedRoleIds(editGroup.getId())) {
            Role r = roleFacade.findById(id);
            if (r != null) {
                selectedRoles.add(r);
            }
        }
    }

    public void save() {
        if (editGroup == null) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        boolean isNew = editGroup.getId() == null || roleGroupFacade.findById(editGroup.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.CREATE)) {
                return;
            }
        } else {
            if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.UPDATE)) {
                return;
            }
        }
        List<UUID> roleIds = selectedRoles.stream().map(Role::getId).toList();
        roleGroupFacade.save(editGroup, roleIds, actor);
        refresh();
        PrimeFaces.current().executeScript("PF('rgDlg').hide()");
    }

    public void softDelete(RoleGroup row) {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.DELETE)) {
            return;
        }
        roleGroupFacade.softDelete(row.getId(), rbacProcedure.currentUserId().orElse(null));
        refresh();
    }

    public List<RoleGroup> getGroups() {
        return groups;
    }

    public RoleGroup getEditGroup() {
        return editGroup;
    }

    public void setEditGroup(RoleGroup editGroup) {
        this.editGroup = editGroup;
    }

    public List<Role> getAssignableRoles() {
        return assignableRoles;
    }

    public List<Role> getSelectedRoles() {
        return selectedRoles;
    }

    public void setSelectedRoles(List<Role> selectedRoles) {
        this.selectedRoles = selectedRoles;
    }
}
