package facadeLocal;

import entity.Users;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface UserFacadeLocal {
    void createUser(Users entity);
    Users editUser(Users entity);
    void remove(Users entity);
    List<Users> usersList();
    Users login(String email, String password);
}
