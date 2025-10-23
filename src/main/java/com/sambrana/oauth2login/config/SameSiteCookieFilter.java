package com.sambrana.oauth2login.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class SameSiteCookieFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(request, response);

        if (response instanceof HttpServletResponse res) {
            // Add SameSite=None so that cookies can be sent between ports (3000 → 8080)
            String header = res.getHeader("Set-Cookie");
            if (header != null && header.contains("XSRF-TOKEN")) {
                res.setHeader("Set-Cookie", header + "; SameSite=None");
            }
        }
    }
}
