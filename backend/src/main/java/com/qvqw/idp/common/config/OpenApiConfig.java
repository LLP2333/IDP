package com.qvqw.idp.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI 全局配置。
 *
 * <p>所有对外接口默认要求携带 JWT，因此这里注册一个全局的 {@code bearerAuth} 安全方案，
 * Swagger UI 上点击 “Authorize” 输入 token 后即可在所有接口上自动带上 {@code Authorization} 头。</p>
 *
 * <p>访问入口：</p>
 * <ul>
 *   <li>Swagger UI：{@code /swagger-ui/index.html}</li>
 *   <li>OpenAPI JSON：{@code /v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    /** 全局 JWT 安全方案名称，与 {@link io.swagger.v3.oas.annotations.security.SecurityRequirement} 引用保持一致。 */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * 注册一个全局 OpenAPI 定义：标题、版本、JWT 安全方案。
     *
     * @return 单例的 OpenAPI 定义
     */
    @Bean
    public OpenAPI idpOpenAPI() {
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("登录接口返回的 JWT，请填入 {token}（不要带 Bearer 前缀）");

        return new OpenAPI()
                .info(new Info()
                        .title("IDP 后台管理系统 API")
                        .description("通用企业级后台管理系统对外提供的 REST 接口文档。")
                        .version("v1")
                        .contact(new Contact().name("IDP Team"))
                        .license(new License().name("Apache 2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, jwtScheme));
    }
}
