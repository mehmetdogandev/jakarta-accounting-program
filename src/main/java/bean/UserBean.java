package bean;

import entity.AppUser;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class UserBean implements Serializable {

    private AppUser user;
    private List<AppUser> users;

    @EJB
    private UserFacadeLocal userFacade;

    public void clearForm() {
        user = new AppUser();
    }

    public void createUser() {
        userFacade.createUser(user);
    }

    public void editUser() {
        userFacade.editUser(user);
    }

    public void updateForm(AppUser u) {
        this.user = u;
    }

    public void deleteUser(AppUser u) {
        userFacade.remove(u);
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
}
