package bean;

import entity.Role;
import entity.RoleGroup;
import enums.Permission;
import enums.Scope;
import facadeLocal.RoleFacadeLocal;
import facadeLocal.RoleGroupFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;
import service.AuditService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @EJB
    private AuditService auditService;

    private List<RoleGroup> groups = Collections.emptyList();
    private Map<UUID, List<String>> linkedRoleNamesByGroupId = Collections.emptyMap();
    private List<Role> assignableRoles = Collections.emptyList();
    private RoleGroup editGroup;
    private RoleGroup detailGroup;
    private List<Role> selectedRoles = new ArrayList<>();
    private String groupSearch = "";
    private String assignableRoleFilter = "";

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.READ)) {
            groups = Collections.emptyList();
            assignableRoles = Collections.emptyList();
            linkedRoleNamesByGroupId = Collections.emptyMap();
            return;
        }
        groups = roleGroupFacade.listActive();
        assignableRoles = roleGroupFacade.listRolesForAssignment();

        Map<UUID, List<String>> map = new LinkedHashMap<>();
        for (RoleGroup g : groups) {
            List<String> names = new ArrayList<>();
            for (UUID rid : roleGroupFacade.linkedRoleIds(g.getId())) {
                Role r = roleFacade.findById(rid);
                if (r != null) {
                    names.add(r.getName());
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            map.put(g.getId(), names);
        }
        linkedRoleNamesByGroupId = map;
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.CREATE)) {
            return;
        }
        editGroup = new RoleGroup();
        selectedRoles = new ArrayList<>();
        assignableRoleFilter = "";
    }

    public void openEdit(RoleGroup row) {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.UPDATE)) {
            return;
        }
        RoleGroup loaded = roleGroupFacade.findById(row.getId());
        this.editGroup = loaded != null ? loaded : row;
        selectedRoles = new ArrayList<>();
        assignableRoleFilter = "";
        for (UUID id : roleGroupFacade.linkedRoleIds(editGroup.getId())) {
            Role r = roleFacade.findById(id);
            if (r != null) {
                selectedRoles.add(r);
            }
        }
    }

    public void openDetail(RoleGroup row) {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.READ)) {
            return;
        }
        RoleGroup loaded = roleGroupFacade.findById(row.getId());
        this.detailGroup = loaded != null ? loaded : row;
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
        auditService.logAction(actor, currentUsername(), isNew ? "CREATE" : "UPDATE", editGroup);
        refresh();
        PrimeFaces.current().executeScript("PF('rgDlg').hide()");
    }

    public void softDelete(RoleGroup row) {
        if (!rbacProcedure.require(Scope.ROLE_GROUP, Permission.DELETE)) {
            return;
        }
        roleGroupFacade.softDelete(row.getId(), rbacProcedure.currentUserId().orElse(null));
        auditService.logAction(rbacProcedure.currentUserId().orElse(null), currentUsername(), "DELETE", row);
        refresh();
    }

    public List<RoleGroup> getGroups() {
        return groups;
    }

    public String getGroupSearch() {
        return groupSearch;
    }

    public void setGroupSearch(String groupSearch) {
        this.groupSearch = groupSearch;
    }

    public List<RoleGroup> getFilteredGroups() {
        String q = groupSearch == null ? "" : groupSearch.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return groups;
        }
        return groups.stream().filter(g -> groupMatches(g, q)).toList();
    }

    private static boolean groupMatches(RoleGroup g, String q) {
        return contains(g.getTitle(), q) || contains(g.getDescription(), q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    public List<String> linkedRolesTopThree(RoleGroup g) {
        List<String> all = linkedRoleNamesByGroupId.getOrDefault(g.getId(), List.of());
        if (all.size() <= 3) {
            return all;
        }
        return all.subList(0, 3);
    }

    public int linkedRolesExtraCount(RoleGroup g) {
        int n = linkedRoleNamesByGroupId.getOrDefault(g.getId(), List.of()).size();
        return Math.max(0, n - 3);
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

    public List<Role> getFilteredAssignableRoles() {
        String f = assignableRoleFilter == null ? "" : assignableRoleFilter.trim().toLowerCase(Locale.ROOT);
        if (f.isEmpty()) {
            return assignableRoles;
        }
        return assignableRoles.stream()
                .filter(r -> (r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(f))
                        || (r.getScope() != null && r.getScope().name().toLowerCase(Locale.ROOT).contains(f)))
                .toList();
    }

    public String getAssignableRoleFilter() {
        return assignableRoleFilter;
    }

    public void setAssignableRoleFilter(String assignableRoleFilter) {
        this.assignableRoleFilter = assignableRoleFilter;
    }

    public RoleGroup getDetailGroup() {
        return detailGroup;
    }

    public List<String> getDetailGroupRoleNames() {
        if (detailGroup == null || detailGroup.getId() == null) {
            return List.of();
        }
        return linkedRoleNamesByGroupId.getOrDefault(detailGroup.getId(), List.of());
    }

    public List<Role> getSelectedRoles() {
        return selectedRoles;
    }

    public void setSelectedRoles(List<Role> selectedRoles) {
        this.selectedRoles = selectedRoles;
    }

    private String currentUsername() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return u instanceof entity.AppUser au ? au.getEmail() : null;
    }
}
