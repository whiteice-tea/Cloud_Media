package com.cloudmedia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.cloudmedia.util.ApiCode;
import com.cloudmedia.util.ApiResponse;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final AppProperties appProperties;
    private final AdminProperties adminProperties;
    private final ObjectMapper objectMapper;

    public AdminTokenFilter(AppProperties appProperties, AdminProperties adminProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.adminProperties = adminProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (appProperties.isPublicMode()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!shouldProtect(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredToken = adminProperties.getToken();
        if (!StringUtils.hasText(configuredToken)) {
            writeForbidden(response, "admin token is not configured");
            return;
        }

        String inputToken = request.getHeader(ADMIN_TOKEN_HEADER);
        if (!StringUtils.hasText(inputToken) || !configuredToken.equals(inputToken)) {
            writeForbidden(response, "admin token invalid");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldProtect(HttpServletRequest request) {
        String method = request.getMethod();
        if (!WRITE_METHODS.contains(method)) {
            return false;
        }
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/")) {
            return false;
        }
        return path.startsWith("/api/media/upload/")
                || path.startsWith("/api/media/")
                || path.startsWith("/api/video/progress/");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ApiCode.FORBIDDEN, message)));
    }
}
