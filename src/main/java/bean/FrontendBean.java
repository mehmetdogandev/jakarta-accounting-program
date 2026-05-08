package bean;

import entity.AppUser;
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

    private AppUser user;
    private List<AppUser> users;

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public List<AppUser> getUsers() {
        users = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            AppUser u = new AppUser();
            u.setId("demo-" + i);
            u.setEmail("user" + i + "@example.com");
            u.setName("A");
            u.setSurname("B");
            users.add(u);
        }
        return users;
    }

    public void setUsers(List<AppUser> users) {
        this.users = users;
    }
}
