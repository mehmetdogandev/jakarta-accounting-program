package bean;

import entity.AppUser;
import facadeLocal.UserFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import service.AuditService;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Named
@ViewScoped
public class LoginBean implements Serializable {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;
    private static final Map<String, AttemptState> ATTEMPTS = new ConcurrentHashMap<>();

    private AppUser user;

    @EJB
    private UserFacadeLocal userFacade;

    @EJB
    private AuditService auditService;

    @Inject
    private FacesContext facesContext;

    public String login() {
        String email = user.getEmail() != null ? user.getEmail().trim() : null;
        String password = user.getPassword();
        HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        AttemptState state = ATTEMPTS.computeIfAbsent(ip, k -> new AttemptState());
        if (state.lockedUntilEpochMs > System.currentTimeMillis()) {
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                    "Giris kilidi", "Bu IP icin gecici kilit aktif. Lutfen 15 dakika sonra tekrar deneyin."));
            return null;
        }

        AppUser u = userFacade.login(email, password);
        if (u != null) {
            var session = facesContext.getExternalContext().getSessionMap();
            session.put("user", u);
            session.put("userId", u.getId());
            state.failCount = 0;
            state.lockedUntilEpochMs = 0L;
            auditService.logLogin(u.getId(), u.getEmail(), ip, userAgent, true);
            return "/admin/dashboard.xhtml?faces-redirect=true";
        }
        state.failCount++;
        if (state.failCount >= MAX_FAILED_ATTEMPTS) {
            state.lockedUntilEpochMs = Instant.now().plusSeconds(LOCK_MINUTES * 60).toEpochMilli();
            state.failCount = 0;
        }
        auditService.logLogin(null, email, ip, userAgent, false);
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

    private static final class AttemptState implements Serializable {
        private int failCount;
        private long lockedUntilEpochMs;
    }
}
