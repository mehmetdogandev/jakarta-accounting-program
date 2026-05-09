package facadeLocal;

import entity.Role;
import entity.RoleGroup;
import jakarta.ejb.Local;

import java.util.List;
import java.util.UUID;

@Local
public interface UserAssignmentFacadeLocal {

    List<Role> listAssignedRoles(String userId);

    List<RoleGroup> listAssignedRoleGroups(String userId);

    List<Role> searchRolesForPicker(String userId, String query, int first, int pageSize);

    long countRolesForPicker(String userId, String query);

    List<RoleGroup> searchRoleGroupsForPicker(String userId, String query, int first, int pageSize);

    long countRoleGroupsForPicker(String userId, String query);

    void assignRole(String userId, UUID roleId, String actorUserId);

    void removeRoleAssignment(String userId, UUID roleId, String actorUserId);

    void assignRoleGroup(String userId, UUID roleGroupId, String actorUserId);

    void removeRoleGroupAssignment(String userId, UUID roleGroupId, String actorUserId);
}
