package bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Rough device class from User-Agent for optional {@code rendered="#{adminDevice.mobile}"} etc.
 * Layout should still rely primarily on CSS breakpoints.
 */
@Named("adminDevice")
@RequestScoped
public class AdminDeviceBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern MOBILE_UA = Pattern.compile(
            "Mobi|Android.*Mobile|iPhone|iPod|webOS|BlackBerry|IEMobile|Opera Mini",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TABLET_UA = Pattern.compile(
            "iPad|Tablet|PlayBook|Silk|Android(?!.*Mobile)",
            Pattern.CASE_INSENSITIVE);

    private transient String cachedUa;

    private String userAgent() {
        if (cachedUa == null) {
            Object req = FacesContext.getCurrentInstance().getExternalContext().getRequest();
            if (req instanceof HttpServletRequest http) {
                String ua = http.getHeader("User-Agent");
                cachedUa = ua != null ? ua : "";
            } else {
                cachedUa = "";
            }
        }
        return cachedUa;
    }

    public boolean isMobile() {
        String ua = userAgent();
        if (ua.isEmpty()) {
            return false;
        }
        if (TABLET_UA.matcher(ua).find()) {
            return false;
        }
        return MOBILE_UA.matcher(ua).find();
    }

    public boolean isTablet() {
        String ua = userAgent();
        if (ua.isEmpty()) {
            return false;
        }
        return TABLET_UA.matcher(ua).find();
    }

    /** Masaüstü / geniş tarayıcı (mobil ve tablet değil). */
    public boolean isWebsite() {
        return !isMobile() && !isTablet();
    }

    public String getCategory() {
        if (isMobile()) {
            return "MOBILE";
        }
        if (isTablet()) {
            return "TABLET";
        }
        return "WEBSITE";
    }

    public String getUserAgentPreview() {
        String ua = userAgent();
        if (ua.isEmpty()) {
            return "";
        }
        return ua.length() > 120 ? ua.substring(0, 120) + "…" : ua;
    }
}
