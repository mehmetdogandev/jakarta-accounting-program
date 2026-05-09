package bean;

import entity.AppUser;
import entity.Role;
import entity.RoleGroup;
import enums.Permission;
import enums.Scope;
import facadeLocal.UserAssignmentFacadeLocal;
import facadeLocal.UserFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import procedure.RbacProcedureBean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Named
@ViewScoped
public class AdminUsersBean implements Serializable {

    private static final int PICKER_PAGE_SIZE = 10;

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private UserAssignmentFacadeLocal assignmentFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<AppUser> users = Collections.emptyList();
    private AppUser editUser;
    private String userSearch = "";

    private UserManagementMode managementMode = UserManagementMode.NEW;

    private List<Role> assignedRoles = Collections.emptyList();
    private List<RoleGroup> assignedRoleGroups = Collections.emptyList();

    private String rolePickerSearch = "";
    private String groupPickerSearch = "";
    private int firstRolePicker;
    private int firstGroupPicker;

    @PostConstruct
    public void init() {
        refresh();
    }

    public void refresh() {
        if (!rbacProcedure.require(Scope.USER, Permission.READ)) {
            users = Collections.emptyList();
            return;
        }
        users = userFacade.usersList();
    }

    public void openNew() {
        if (!rbacProcedure.require(Scope.USER, Permission.CREATE)) {
            return;
        }
        managementMode = UserManagementMode.NEW;
        editUser = new AppUser();
        clearAssignmentState();
    }

    public void openEdit(AppUser row) {
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        managementMode = UserManagementMode.EDIT;
        AppUser loaded = userFacade.findById(row.getId());
        this.editUser = loaded != null ? loaded : row;
        loadAssignments();
        resetPickerPaging();
    }

    public void openDetail(AppUser row) {
        if (!rbacProcedure.require(Scope.USER, Permission.READ)) {
            return;
        }
        managementMode = UserManagementMode.DETAIL;
        AppUser loaded = userFacade.findById(row.getId());
        this.editUser = loaded != null ? loaded : row;
        loadAssignments();
        resetPickerPaging();
    }

    private void clearAssignmentState() {
        assignedRoles = Collections.emptyList();
        assignedRoleGroups = Collections.emptyList();
        rolePickerSearch = "";
        groupPickerSearch = "";
        firstRolePicker = 0;
        firstGroupPicker = 0;
    }

    private void resetPickerPaging() {
        firstRolePicker = 0;
        firstGroupPicker = 0;
    }

    private void loadAssignments() {
        if (editUser == null || editUser.getId() == null) {
            assignedRoles = Collections.emptyList();
            assignedRoleGroups = Collections.emptyList();
            return;
        }
        assignedRoles = List.copyOf(assignmentFacade.listAssignedRoles(editUser.getId()));
        assignedRoleGroups = List.copyOf(assignmentFacade.listAssignedRoleGroups(editUser.getId()));
    }

    public void refreshRolePicker() {
        // ajax: reset to first page when search changes
        firstRolePicker = 0;
    }

    public void refreshGroupPicker() {
        firstGroupPicker = 0;
    }

    public List<Role> getPickerRoles() {
        if (editUser == null || editUser.getId() == null || !canManageAssignments()) {
            return Collections.emptyList();
        }
        return assignmentFacade.searchRolesForPicker(editUser.getId(), rolePickerSearch, firstRolePicker, PICKER_PAGE_SIZE);
    }

    public long getPickerRolesCount() {
        if (editUser == null || editUser.getId() == null || !canManageAssignments()) {
            return 0L;
        }
        return assignmentFacade.countRolesForPicker(editUser.getId(), rolePickerSearch);
    }

    public List<RoleGroup> getPickerRoleGroups() {
        if (editUser == null || editUser.getId() == null || !canManageAssignments()) {
            return Collections.emptyList();
        }
        return assignmentFacade.searchRoleGroupsForPicker(editUser.getId(), groupPickerSearch, firstGroupPicker, PICKER_PAGE_SIZE);
    }

    public long getPickerRoleGroupsCount() {
        if (editUser == null || editUser.getId() == null || !canManageAssignments()) {
            return 0L;
        }
        return assignmentFacade.countRoleGroupsForPicker(editUser.getId(), groupPickerSearch);
    }

    public void pickerRolesNext() {
        long total = getPickerRolesCount();
        if (firstRolePicker + PICKER_PAGE_SIZE < total) {
            firstRolePicker += PICKER_PAGE_SIZE;
        }
    }

    public void pickerRolesPrev() {
        firstRolePicker = Math.max(0, firstRolePicker - PICKER_PAGE_SIZE);
    }

    public void pickerGroupsNext() {
        long total = getPickerRoleGroupsCount();
        if (firstGroupPicker + PICKER_PAGE_SIZE < total) {
            firstGroupPicker += PICKER_PAGE_SIZE;
        }
    }

    public void pickerGroupsPrev() {
        firstGroupPicker = Math.max(0, firstGroupPicker - PICKER_PAGE_SIZE);
    }

    public void assignPickerRole(Role role) {
        if (role == null || editUser == null || editUser.getId() == null) {
            return;
        }
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        assignmentFacade.assignRole(editUser.getId(), role.getId(), actor);
        loadAssignments();
    }

    public void removeAssignedRole(Role role) {
        if (role == null || editUser == null || editUser.getId() == null) {
            return;
        }
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        assignmentFacade.removeRoleAssignment(editUser.getId(), role.getId(), actor);
        loadAssignments();
    }

    public void assignPickerRoleGroup(RoleGroup group) {
        if (group == null || editUser == null || editUser.getId() == null) {
            return;
        }
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        assignmentFacade.assignRoleGroup(editUser.getId(), group.getId(), actor);
        loadAssignments();
    }

    public void removeAssignedRoleGroup(RoleGroup group) {
        if (group == null || editUser == null || editUser.getId() == null) {
            return;
        }
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        assignmentFacade.removeRoleGroupAssignment(editUser.getId(), group.getId(), actor);
        loadAssignments();
    }

    /**
     * Rol / rol grubu atama (picker + kaldırma) yalnızca düzenleme modunda ve USER+UPDATE ile.
     */
    public boolean canManageAssignments() {
        return managementMode == UserManagementMode.EDIT
                && editUser != null
                && editUser.getId() != null;
    }

    public boolean isProfileEditable() {
        return managementMode == UserManagementMode.NEW
                || managementMode == UserManagementMode.EDIT;
    }

    public String getManagementDialogTitle() {
        if (editUser == null) {
            return "Kullanıcı";
        }
        return switch (managementMode) {
            case NEW -> "Yeni kullanıcı";
            case EDIT -> "Kullanıcıyı düzenle";
            case DETAIL -> "Kullanıcı detayı";
        };
    }

    public String getManagementModeLabel() {
        return switch (managementMode) {
            case NEW -> "Yeni kayıt";
            case EDIT -> "Düzenleme";
            case DETAIL -> "Salt okunur";
        };
    }

    public void save() {
        if (editUser == null) {
            return;
        }
        String actor = rbacProcedure.currentUserId().orElse(null);
        boolean isNew = editUser.getId() == null || userFacade.findById(editUser.getId()) == null;
        if (isNew) {
            if (!rbacProcedure.require(Scope.USER, Permission.CREATE)) {
                return;
            }
            if (userFacade.emailExists(editUser.getEmail(), null)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt", "Bu e-posta zaten kullanılıyor."));
                return;
            }
            editUser.setCreatedBy(actor);
            editUser.setLastUpdatedBy(actor);
            userFacade.createUser(editUser);
        } else {
            if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
                return;
            }
            if (userFacade.emailExists(editUser.getEmail(), editUser.getId())) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt", "Bu e-posta başka kullanıcıda kayıtlı."));
                return;
            }
            editUser.setLastUpdatedBy(actor);
            userFacade.editUser(editUser);
        }
        refresh();
        PrimeFaces.current().executeScript("PF('userMgmtDlg').hide()");
    }

    public void softDelete(AppUser row) {
        if (!rbacProcedure.require(Scope.USER, Permission.DELETE)) {
            return;
        }
        var actor = rbacProcedure.currentUserId();
        if (actor.isPresent() && actor.get().equals(row.getId())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Silme", "Kendi hesabınızı silemezsiniz."));
            return;
        }
        userFacade.softDeleteUser(row.getId(), actor.orElse(null));
        refresh();
    }

    public List<AppUser> getUsers() {
        return users;
    }

    public String getUserSearch() {
        return userSearch;
    }

    public void setUserSearch(String userSearch) {
        this.userSearch = userSearch;
    }

    public List<AppUser> getFilteredUsers() {
        String q = userSearch == null ? "" : userSearch.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return users;
        }
        return users.stream().filter(u -> matchesSearch(u, q)).toList();
    }

    private static boolean matchesSearch(AppUser u, String q) {
        return contains(u.getEmail(), q) || contains(u.getName(), q) || contains(u.getSurname(), q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    public AppUser getEditUser() {
        return editUser;
    }

    public void setEditUser(AppUser editUser) {
        this.editUser = editUser;
    }

    public UserManagementMode getManagementMode() {
        return managementMode;
    }

    public List<Role> getAssignedRoles() {
        return assignedRoles;
    }

    public List<RoleGroup> getAssignedRoleGroups() {
        return assignedRoleGroups;
    }

    public String getRolePickerSearch() {
        return rolePickerSearch;
    }

    public void setRolePickerSearch(String rolePickerSearch) {
        this.rolePickerSearch = rolePickerSearch;
    }

    public String getGroupPickerSearch() {
        return groupPickerSearch;
    }

    public void setGroupPickerSearch(String groupPickerSearch) {
        this.groupPickerSearch = groupPickerSearch;
    }

    public int getFirstRolePicker() {
        return firstRolePicker;
    }

    public int getFirstGroupPicker() {
        return firstGroupPicker;
    }

    public int getPickerPageSize() {
        return PICKER_PAGE_SIZE;
    }

    public boolean isPickerRolesHasNext() {
        return firstRolePicker + PICKER_PAGE_SIZE < getPickerRolesCount();
    }

    public boolean isPickerRolesHasPrev() {
        return firstRolePicker > 0;
    }

    public boolean isPickerGroupsHasNext() {
        return firstGroupPicker + PICKER_PAGE_SIZE < getPickerRoleGroupsCount();
    }

    public boolean isPickerGroupsHasPrev() {
        return firstGroupPicker > 0;
    }

    public int getPickerRolesPageStart() {
        long c = getPickerRolesCount();
        if (c == 0) {
            return 0;
        }
        return firstRolePicker + 1;
    }

    public int getPickerRolesPageEnd() {
        return (int) Math.min(firstRolePicker + PICKER_PAGE_SIZE, getPickerRolesCount());
    }

    public int getPickerGroupsPageEnd() {
        return (int) Math.min(firstGroupPicker + PICKER_PAGE_SIZE, getPickerRoleGroupsCount());
    }

    public boolean isDetailMode() {
        return managementMode == UserManagementMode.DETAIL;
    }

    public boolean isNewMode() {
        return managementMode == UserManagementMode.NEW;
    }

    public boolean isEditMode() {
        return managementMode == UserManagementMode.EDIT;
    }
}
