package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.AuthProperties;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final AuthProperties properties = new AuthProperties();

    JwtTokenProviderTest() {
        properties.getJwt().setSecret("test-secret-test-secret-test-secret-test-secret");
        properties.getJwt().setExpires(60);
    }

    @Test
    void issueAndParse() {
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        JwtTokenProvider.IssuedToken issued = provider.issue(1L, "admin");
        JwtTokenProvider.ParsedToken parsed = provider.parse(issued.token());
        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.jti()).isEqualTo(issued.jti());
        assertThat(issued.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void rejectExpiredToken() throws Exception {
        properties.getJwt().setExpires(-1);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        JwtTokenProvider.IssuedToken issued = provider.issue(1L, "admin");
        assertThatThrownBy(() -> provider.parse(issued.token()))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
