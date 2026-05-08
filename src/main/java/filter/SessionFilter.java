package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(filterName = "SessionFilter", urlPatterns = {"/panel/*", "/app/*"})
public class SessionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String ctx = request.getContextPath();
        String loginPath = ctx + "/login";
        String panelPath = ctx + "/panel/index";

        boolean loggedIn = request.getSession().getAttribute("user") != null
                || request.getSession().getAttribute("userId") != null;
        String uri = request.getRequestURI();
        boolean loginRequest = uri.equals(loginPath) || uri.equals(ctx + "/login.xhtml");

        if (loggedIn || loginRequest) {
            if (loginRequest && loggedIn) {
                response.sendRedirect(panelPath + ".xhtml");
            } else {
                chain.doFilter(request, response);
            }
        } else {
            if (isAJAXRequest(request)) {
                response.setContentType("text/xml");
                response.setCharacterEncoding("UTF-8");
                response.getWriter()
                        .write("<?xml version='1.0' encoding='UTF-8'?>"
                                + "<partial-response><redirect url='" + loginPath + ".xhtml'/></partial-response>");
            } else {
                response.sendRedirect(loginPath + ".xhtml");
            }
        }
    }

    private boolean isAJAXRequest(HttpServletRequest request) {
        String facesRequest = request.getHeader("Faces-Request");
        return "partial/ajax".equals(facesRequest);
    }
}