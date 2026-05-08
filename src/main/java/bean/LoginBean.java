package bean;

import entity.Users;
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
    private Users user;
    @EJB
    UserFacadeLocal userFacade;
    @Inject
    FacesContext facesContext;


    public String login() {
        String email = user.getEmail() != null ? user.getEmail().trim() : null;
        String password = user.getPassword();

        Users u = userFacade.login(email, password);
        if (u != null) {
            facesContext.getExternalContext().getSessionMap().put("user", u);
            return "/panel/index.xhtml?faces-redirect=true";
        } else {
            FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login failed", "Email veya parola hatali.");
            facesContext.addMessage(null, msg);
            return null;
        }
    }

    public Users getUser() {
        if (user == null) {
            user = new Users();
        }
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }
}
