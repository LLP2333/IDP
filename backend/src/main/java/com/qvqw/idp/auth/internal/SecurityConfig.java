package com.qvqw.idp.auth.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvqw.idp.auth.AuthProperties;
import com.qvqw.idp.common.api.R;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 主配置：JWT 无状态认证 + CORS + 异常 JSON 响应。
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    /** 无需登录即可访问的路径白名单。 */
    private static final String[] WHITELIST = {
            "/auth/login",
            "/auth/logout",
            "/auth/captcha",
            "/system/option/site",
            "/system/option/login",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/file/local/**"
    };

    /**
     * 全局密码哈希器，使用 BCrypt（带 salt + 自适应代价）。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 配置：当前阶段允许所有 origin / method / header，方便本地与多端联调。
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 构造 Spring Security 的过滤器链：
     *
     * <ul>
     *   <li>禁用 CSRF / Session（纯 JWT 无状态）；</li>
     *   <li>白名单放行登录、OpenAPI、actuator 等；</li>
     *   <li>认证 / 鉴权异常统一通过 JSON 返回 {@link R} 结构；</li>
     *   <li>在 {@code UsernamePasswordAuthenticationFilter} 之前插入 {@link JwtAuthenticationFilter}。</li>
     * </ul>
     *
     * @param http      Spring Security HTTP 配置入口
     * @param jwtFilter 自定义 JWT 解析过滤器
     * @return 已配置的 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   ApplicationEventPublisher eventPublisher) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AuditLogFilter(eventPublisher), JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 401 未认证响应：返回 {@link R} 形态的 JSON。
     */
    private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                R.fail(R.CODE_UNAUTHORIZED, "未登录或登录已过期"), objectMapper);
    }

    /**
     * 403 无权限响应：返回 {@link R} 形态的 JSON。
     */
    private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) -> writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                R.fail(R.CODE_FORBIDDEN, "无访问权限"), objectMapper);
    }

    /**
     * 将业务体序列化为 JSON 并写入响应。
     */
    private void writeJson(HttpServletResponse response, int status, Object body, ObjectMapper mapper) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(body));
    }
}
