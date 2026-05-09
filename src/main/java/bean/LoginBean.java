package bean;

import entity.AppUser;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class LoginBean implements Serializable {

    private AppUser user;

    @EJB
    private UserFacadeLocal userFacade;

    @Inject
    private FacesContext facesContext;

    public String login() {
        String email = user.getEmail() != null ? user.getEmail().trim() : null;
        String password = user.getPassword();

        AppUser u = userFacade.login(email, password);
        if (u != null) {
            var session = facesContext.getExternalContext().getSessionMap();
            session.put("user", u);
            session.put("userId", u.getId());
            return "/admin/dashboard.xhtml?faces-redirect=true";
        }
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Email veya parola hatali.");
        facesContext.addMessage(null, msg);
        return null;
    }

    public AppUser getUser() {
        if (user == null) {
            user = new AppUser();
        }
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }
}
