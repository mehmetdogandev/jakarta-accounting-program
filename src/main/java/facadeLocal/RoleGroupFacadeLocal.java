package facadeLocal;

import entity.Role;
import entity.RoleGroup;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface RoleGroupFacadeLocal {

    List<RoleGroup> listActive();

    RoleGroup findById(UUID id);

    List<UUID> linkedRoleIds(UUID groupId);

    List<Role> listRolesForAssignment();

    void save(RoleGroup group, List<UUID> roleIds, String actorUserId);

    void softDelete(UUID groupId, String actorUserId);
}
