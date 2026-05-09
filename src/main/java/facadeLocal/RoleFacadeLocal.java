package facadeLocal;

import entity.Role;
import enums.Permission;
import enums.Scope;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface RoleFacadeLocal {

    List<Role> listByScope(Scope scope);

    Role findById(UUID id);

    List<Permission> listPermissions(UUID roleId);

    void save(Role role, List<Permission> permissions, String actorUserId);

    void softDelete(UUID roleId, String actorUserId);
}
