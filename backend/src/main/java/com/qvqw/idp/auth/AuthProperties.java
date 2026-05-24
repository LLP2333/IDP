package com.qvqw.idp.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证配置。
 */
@ConfigurationProperties(prefix = "idp.auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();

    public Jwt getJwt() {
        return jwt;
    }

    public static class Jwt {

        /** HS256 secret，长度需 >= 32 字节。 */
        private String secret = "idp-default-jwt-secret-key-please-override-in-production-environment";

        /** 过期时间（秒）。 */
        private long expires = 3600;

        /** issuer 声明。 */
        private String issuer = "idp";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpires() {
            return expires;
        }

        public void setExpires(long expires) {
            this.expires = expires;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }
}
