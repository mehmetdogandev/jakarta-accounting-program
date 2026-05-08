package bean;

import entity.Users;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class FrontendBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Users user;
    private List<Users> users;

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public List<Users> getUsers() {
        users = new ArrayList<>();
        users.add(new Users(1L,"A","B","C","D"));
        users.add(new Users(2L,"A","B","C","D"));
        users.add(new Users(3L,"A","B","C","D"));
        users.add(new Users(4L,"A","B","C","D"));
        return users;
    }

    public void setUsers(List<Users> users) {
        this.users = users;
    }
}
