package procedure;

import enums.Permission;
import enums.Scope;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import service.AuthorizationServiceLocal;

import java.util.Optional;

/**
 * Sunucu tarafı RBAC guard — tRPC {@code rbacProcedure(scope, permission)} ile aynı semantik.
 */
@Stateless
public class RbacProcedureBean {

    @EJB
    private AuthorizationServiceLocal authorizationService;

    public Optional<String> currentUserId() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return Optional.empty();
        }
        Object id = fc.getExternalContext().getSessionMap().get("userId");
        if (id instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    /**
     * @return true yetki var veya FacesContext yok (EJB test); false yetkisiz (FacesMessage eklenir)
     */
    public boolean require(Scope scope, Permission permission) {
        FacesContext fc = FacesContext.getCurrentInstance();
        Optional<String> userId = currentUserId();
        if (userId.isEmpty()) {
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Yetki", "Oturum bulunamadı."));
            }
            return false;
        }
        if (!authorizationService.can(userId.get(), permission, scope)) {
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Yetki",
                        "Bu işlem için " + scope.name() + " kapsamında " + permission.name() + " izni gerekiyor."));
            }
            return false;
        }
        return true;
    }

    public boolean require(String scopeName, String permissionName) {
        try {
            Scope scope = Scope.valueOf(scopeName.trim().toUpperCase());
            Permission permission = Permission.valueOf(permissionName.trim().toUpperCase());
            return require(scope, permission);
        } catch (IllegalArgumentException ex) {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc != null) {
                fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Yetki", "Geçersiz scope veya permission."));
            }
            return false;
        }
    }
}
