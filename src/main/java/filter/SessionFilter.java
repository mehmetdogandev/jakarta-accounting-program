package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(
        filterName = "SessionFilter",
        urlPatterns = {
                "/panel/*", "/app/*", "/admin/*",
                "/login", "/login.xhtml", "/register", "/register.xhtml"
        })
public class SessionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String ctx = request.getContextPath();
        String loginPath = ctx + "/login";
        String registerPath = ctx + "/register";
        String dashboardPath = ctx + "/admin/dashboard";

        boolean loggedIn = request.getSession().getAttribute("user") != null
                || request.getSession().getAttribute("userId") != null;
        String uri = request.getRequestURI();
        boolean loginRequest = uri.equals(loginPath) || uri.equals(ctx + "/login.xhtml");
        boolean registerRequest = uri.equals(registerPath) || uri.equals(ctx + "/register.xhtml");

        if (loginRequest || registerRequest) {
            if (loggedIn) {
                response.sendRedirect(dashboardPath + ".xhtml");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (!loggedIn) {
            if (isAJAXRequest(request)) {
                response.setContentType("text/xml");
                response.setCharacterEncoding("UTF-8");
                response.getWriter()
                        .write("<?xml version='1.0' encoding='UTF-8'?>"
                                + "<partial-response><redirect url='" + loginPath + ".xhtml'/></partial-response>");
            } else {
                response.sendRedirect(loginPath + ".xhtml");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAJAXRequest(HttpServletRequest request) {
        String facesRequest = request.getHeader("Faces-Request");
        return "partial/ajax".equals(facesRequest);
    }
}
