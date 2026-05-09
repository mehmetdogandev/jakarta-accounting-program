package bean;

import entity.AppUser;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import service.AuditService;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class UserBean implements Serializable {

    private AppUser user;
    private List<AppUser> users;

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private AuditService auditService;

    public void clearForm() {
        user = new AppUser();
    }

    public void createUser() {
        userFacade.createUser(user);
        auditService.logAction(currentUserId(), currentUsername(), "CREATE", user);
    }

    public void editUser() {
        userFacade.editUser(user);
        auditService.logAction(currentUserId(), currentUsername(), "UPDATE", user);
    }

    public void updateForm(AppUser u) {
        this.user = u;
    }

    public void deleteUser(AppUser u) {
        userFacade.remove(u);
        auditService.logAction(currentUserId(), currentUsername(), "DELETE", u);
    }

    public AppUser getUser() {
        if (user == null) {
            user = new AppUser();
        }
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public List<AppUser> getUsers() {
        users = userFacade.usersList();
        return users;
    }

    public void setUsers(List<AppUser> users) {
        this.users = users;
    }

    private String currentUserId() {
        Object id = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("userId");
        return id instanceof String s ? s : null;
    }

    private String currentUsername() {
        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return u instanceof AppUser au ? au.getEmail() : null;
    }
}
