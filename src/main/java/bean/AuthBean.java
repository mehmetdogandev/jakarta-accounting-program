package bean;

import entity.AppUser;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import service.AuditService;

import java.io.Serializable;
import java.util.Map;

@Named("authBean")
@RequestScoped
public class AuthBean implements Serializable {

    @EJB
    private AuditService auditService;

    public String logout() {
        Map<String, Object> session = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        String userId = session.get("userId") instanceof String id ? id : null;
        String username = null;
        Object userObj = session.get("user");
        if (userObj instanceof AppUser u) {
            username = u.getEmail();
        }
        auditService.log(userId, username, "LOGOUT", "Auth", null, null, null, null);
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login.xhtml?faces-redirect=true";
    }
}
