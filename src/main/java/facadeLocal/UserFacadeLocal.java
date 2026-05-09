package facadeLocal;

import entity.AppUser;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface UserFacadeLocal {

    void createUser(AppUser entity);

    AppUser editUser(AppUser entity);

    void remove(AppUser entity);

    List<AppUser> usersList();

    AppUser login(String email, String password);

    AppUser findById(String id);

    boolean emailExists(String email, String excludeUserId);

    void softDeleteUser(String targetUserId, String actorUserId);
}
