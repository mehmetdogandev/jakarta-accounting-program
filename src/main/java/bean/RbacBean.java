package bean;

import enums.Permission;
import enums.Scope;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import service.AuthorizationServiceLocal;

import java.io.Serializable;

/**
 * Facelets EL için RBAC sorguları: {@code #{rbac.can('USER','ACCESS')}}.
 */
@Named("rbac")
@RequestScoped
public class RbacBean implements Serializable {

    @EJB
    private AuthorizationServiceLocal authorizationService;

    public boolean can(String scopeName, String permissionName) {
        try {
            Scope scope = Scope.valueOf(scopeName.trim().toUpperCase());
            Permission permission = Permission.valueOf(permissionName.trim().toUpperCase());
            return currentUserId()
                    .map(uid -> authorizationService.can(uid, permission, scope))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private java.util.Optional<String> currentUserId() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return java.util.Optional.empty();
        }
        Object id = fc.getExternalContext().getSessionMap().get("userId");
        if (id instanceof String s && !s.isBlank()) {
            return java.util.Optional.of(s);
        }
        return java.util.Optional.empty();
    }
}
