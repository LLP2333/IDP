package com.qvqw.idp.auth.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvqw.idp.common.security.UserContext;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.role.RoleService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 解析 JWT 并装填 {@link SecurityContextHolder} 与 {@link UserContextHolder}。
 *
 * <p>对每个请求至多解析一次：</p>
 * <ol>
 *   <li>从 {@code Authorization: Bearer <token>} 头取出 token，无 token 时直接放行；</li>
 *   <li>校验签名并检查 jti 是否仍在 Redis（未被注销）；</li>
 *   <li>查询用户角色码 / 权限码（带 Redis 缓存），写入 SecurityContext / UserContextHolder；</li>
 *   <li>请求结束在 {@code finally} 块中清空两个 ThreadLocal，避免线程复用泄露。</li>
 * </ol>
 *
 * <p>用户的角色 / 权限若每次请求都查库，会显著影响吞吐，因此用 Redis 做按用户缓存：</p>
 * <ul>
 *   <li>{@code idp:auth:roles:<userId>}：角色码 JSON 数组，TTL 5 分钟；</li>
 *   <li>{@code idp:auth:perms:<userId>}：权限码 JSON 数组，TTL 5 分钟。</li>
 * </ul>
 * 当角色 / 权限发生变更时，相应模块需主动清理这两个 key；这里只读不主动失效。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    static final String ROLE_CACHE_PREFIX = "idp:auth:roles:";
    static final String PERM_CACHE_PREFIX = "idp:auth:perms:";
    private static final TypeReference<Set<String>> SET_OF_STRING = new TypeReference<>() {
    };

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final RoleService roleService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   TokenStore tokenStore,
                                   RoleService roleService,
                                   @Autowired(required = false) StringRedisTemplate redisTemplate,
                                   @Autowired(required = false) ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.tokenStore = tokenStore;
        this.roleService = roleService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
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
                    Set<String> roleCodes = loadRoleCodes(parsed.userId());
                    Set<String> permissionCodes = loadPermissionCodes(parsed.userId());
                    UserContext context = new UserContext(parsed.userId(), parsed.username(), null,
                            roleCodes, permissionCodes);
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

    /** 从请求头中提取裸 token。 */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }

    private Set<String> loadRoleCodes(Long userId) {
        Set<String> cached = readCache(ROLE_CACHE_PREFIX + userId);
        if (cached != null) {
            return cached;
        }
        Set<String> codes = roleService.listCodesByUserId(userId);
        writeCache(ROLE_CACHE_PREFIX + userId, codes);
        return codes;
    }

    private Set<String> loadPermissionCodes(Long userId) {
        Set<String> cached = readCache(PERM_CACHE_PREFIX + userId);
        if (cached != null) {
            return cached;
        }
        Set<String> codes = roleService.listPermissionCodesByUserId(userId);
        writeCache(PERM_CACHE_PREFIX + userId, codes);
        return codes;
    }

    private Set<String> readCache(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, SET_OF_STRING);
        } catch (Exception ex) {
            return null;
        }
    }

    private void writeCache(String key, Set<String> value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value == null ? new HashSet<>() : value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception ex) {
            log.debug("[auth] 写 Redis 缓存失败 {}: {}", key, ex.getMessage());
        }
    }
}
