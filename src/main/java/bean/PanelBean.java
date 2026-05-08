package bean;

import entity.AppUser;
import enums.Permission;
import enums.Scope;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import service.AuthorizationServiceLocal;

import java.io.Serial;
import java.io.Serializable;

@Named
@ViewScoped
public class PanelBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @EJB
    private AuthorizationServiceLocal authorizationService;

    public boolean isCanReadDashboard() {
        AppUser u = currentUser();
        return u != null && authorizationService.can(u.getId(), Permission.READ);
    }

    public boolean isCanAccessPanel() {
        AppUser u = currentUser();
        return u != null && authorizationService.can(u.getId(), Permission.ACCESS);
    }

    /**
     * Example: permission check restricted to roles with {@link Scope#USER}.
     */
    public boolean isCanReadAsUserScope() {
        AppUser u = currentUser();
        return u != null && authorizationService.can(u.getId(), Permission.READ, Scope.USER);
    }

    private AppUser currentUser() {
        Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("user");
        return o instanceof AppUser appUser ? appUser : null;
    }
}
