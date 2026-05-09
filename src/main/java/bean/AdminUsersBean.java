package bean;

import entity.AppUser;
import enums.Permission;
import enums.Scope;
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

@Named
@ViewScoped
public class AdminUsersBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private RbacProcedureBean rbacProcedure;

    private List<AppUser> users = Collections.emptyList();
    private AppUser editUser;

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
        editUser = new AppUser();
    }

    public void openEdit(AppUser row) {
        if (!rbacProcedure.require(Scope.USER, Permission.UPDATE)) {
            return;
        }
        AppUser loaded = userFacade.findById(row.getId());
        this.editUser = loaded != null ? loaded : row;
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
        PrimeFaces.current().executeScript("PF('userDlg').hide()");
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

    public AppUser getEditUser() {
        return editUser;
    }

    public void setEditUser(AppUser editUser) {
        this.editUser = editUser;
    }
}
