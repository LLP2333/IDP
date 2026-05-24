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
 * 解析 JWT 并装填 {@link SecurityContextHolder} 与 {@link UserContextHolder}。
 *
 * <p>对每个请求至多解析一次：</p>
 * <ol>
 *   <li>从 {@code Authorization: Bearer <token>} 头取出 token，无 token 时直接放行；</li>
 *   <li>校验签名并检查 jti 是否仍在 Redis（未被注销）；</li>
 *   <li>查询用户角色码，写入 SecurityContext / UserContextHolder；</li>
 *   <li>请求结束在 {@code finally} 块中清空两个 ThreadLocal，避免线程复用泄露。</li>
 * </ol>
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

    /**
     * 真正的过滤入口：解析 token → 装填上下文 → 调用下游 → 清理。
     */
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

    /**
     * 从请求头中提取裸 token（去掉 {@code Bearer } 前缀）。
     *
     * @param request HTTP 请求
     * @return token；不存在或格式不正确时返回 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }
}
