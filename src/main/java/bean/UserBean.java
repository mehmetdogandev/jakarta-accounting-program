package bean;

import entity.Users;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class UserBean implements Serializable {
    private Users user;
    private List<Users> users;

    @EJB
    private UserFacadeLocal userFacade;

    public void clearForm() {
        user = new Users();
    }

    public void createUser() {
        userFacade.createUser(user);
        System.out.println("User created");
    }

    public void editUser() {
        userFacade.editUser(user);
        System.out.println("User edited");
    }

    public void updateForm(Users u) {
        this.user = u;
    }
    public void deleteUser(Users u) {
        userFacade.remove(u);
        System.out.println("User deleted");
    }

    public Users getUser() {
        if (user == null) {
            user = new Users();
        }
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public List<Users> getUsers() {
        users = userFacade.usersList();
        return users;
    }

    public void setUsers(List<Users> users) {
        this.users = users;
    }
}
