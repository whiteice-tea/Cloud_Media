package com.cloudmedia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.cloudmedia.service.SignedUrlService;
import com.cloudmedia.util.JwtUserClaims;
import com.cloudmedia.util.UserContext;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATTERNS = List.of("/api/auth/register", "/api/auth/login");

    private final AppProperties appProperties;
    private final JwtUtil jwtUtil;
    private final SignedUrlService signedUrlService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(AppProperties appProperties, JwtUtil jwtUtil, SignedUrlService signedUrlService) {
        this.appProperties = appProperties;
        this.jwtUtil = jwtUtil;
        this.signedUrlService = signedUrlService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (appProperties.isPublicMode()) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = extractBearerToken(request);
            if (StringUtils.hasText(token)) {
                JwtUserClaims claims = jwtUtil.parseToken(token);
                applyAuthentication(claims);
            } else if (isMediaAccessPath(request)) {
                String accessToken = request.getParameter("accessToken");
                if (StringUtils.hasText(accessToken)) {
                    Long userId = signedUrlService.verifyAccessToken(accessToken, request);
                    if (userId != null) {
                        JwtUserClaims claims = new JwtUserClaims(userId, "signed-url-user");
                        applyAuthentication(claims);
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private void applyAuthentication(JwtUserClaims claims) {
        UserContext.set(claims);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                claims.getUsername(), null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private boolean isMediaAccessPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return pathMatcher.match("/api/media/stream/video/*", path)
                || pathMatcher.match("/api/media/view/doc/*", path);
    }
}
