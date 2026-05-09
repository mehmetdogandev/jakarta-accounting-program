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
public class RegisterBean implements Serializable {

    @EJB
    private UserFacadeLocal userFacade;

    @Inject
    private FacesContext facesContext;

    private AppUser user;

    public String register() {
        String email = user.getEmail() != null ? user.getEmail().trim() : "";
        String password = user.getPassword();
        if (email.isEmpty() || password == null || password.isBlank()) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt", "E-posta ve şifre zorunlu."));
            return null;
        }
        user.setEmail(email);
        if (userFacade.emailExists(email, null)) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Kayıt", "Bu e-posta zaten kayıtlı."));
            return null;
        }
        userFacade.createUser(user);
        facesContext.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Kayıt", "Hesap oluşturuldu. Giriş yapabilirsiniz."));
        return "/login.xhtml?faces-redirect=true";
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
