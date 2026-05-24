package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.UserContext;
import com.qvqw.idp.auth.UserContextHolder;
import com.qvqw.idp.role.RoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 解析 JWT 并装填 SecurityContext + UserContext。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final RoleService roleService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   TokenStore tokenStore,
                                   RoleService roleService) {
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.roleService = roleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                JwtTokenProvider.ParsedToken parsed = tokenProvider.parse(token);
                if (!tokenStore.exists(parsed.jti())) {
                    log.debug("JWT jti 已失效: {}", parsed.jti());
                } else {
                    Set<String> roleCodes = roleService.listCodesByUserId(parsed.userId());
                    UserContext context = new UserContext(parsed.userId(), parsed.username(), null, roleCodes);
                    UserContextHolder.set(context);
                    List<SimpleGrantedAuthority> authorities = roleCodes.stream()
                            .map(c -> new SimpleGrantedAuthority("ROLE_" + c))
                            .toList();
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(parsed.username(), null, authorities);
                    auth.setDetails(parsed);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                log.debug("JWT 解析失败: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
