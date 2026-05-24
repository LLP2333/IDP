package com.qvqw.idp.user.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.user.UserService;
import com.qvqw.idp.user.model.query.UserQuery;
import com.qvqw.idp.user.model.resp.UserResp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户分页查询集成测试。
 *
 * <p>主要用于回归校验：当 {@link UserQuery} 全部字段为 {@code null} / 仅部分字段非空时，
 * 通过 JPA Criteria 拼装出的 SQL 在真实数据库上不会再触发
 * {@code function lower(bytea) does not exist}（Hibernate 7 + PostgreSQL 兼容性问题）。
 * 测试在 H2 (PostgreSQL 模式) 下也能完整跑通 SQL，足以验证条件拼装正确。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServicePageIntegrationTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    @SuppressWarnings("unused")
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void pageWithEmptyQueryShouldReturnSeededAdmin() {
        PageResp<UserResp> resp = userService.page(null, 1, 10);
        assertThat(resp).isNotNull();
        assertThat(resp.getList()).extracting(UserResp::getUsername).contains("admin");
    }

    @Test
    void pageWithNullFieldsShouldNotPushNullToSql() {
        UserQuery query = new UserQuery();
        PageResp<UserResp> resp = userService.page(query, 1, 10);
        assertThat(resp).isNotNull();
        assertThat(resp.getList()).isNotEmpty();
    }

    @Test
    void pageWithUsernameFilterShouldMatch() {
        UserQuery query = new UserQuery();
        query.setUsername("adm");
        PageResp<UserResp> resp = userService.page(query, 1, 10);
        assertThat(resp.getList()).extracting(UserResp::getUsername).contains("admin");
    }

    @Test
    void pageWithStatusOnlyShouldMatch() {
        UserQuery query = new UserQuery();
        query.setStatus(1);
        PageResp<UserResp> resp = userService.page(query, 1, 10);
        assertThat(resp.getList()).isNotEmpty();
        assertThat(resp.getList()).allMatch(u -> u.getStatus() == 1);
    }

    @Test
    void pageWithUsernameAndStatusShouldMatch() {
        UserQuery query = new UserQuery();
        query.setUsername("admin");
        query.setStatus(1);
        PageResp<UserResp> resp = userService.page(query, 1, 10);
        assertThat(resp.getList()).extracting(UserResp::getUsername).contains("admin");
    }
}
