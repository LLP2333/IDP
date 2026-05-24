package com.qvqw.idp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvqw.idp.auth.internal.JwtTokenProvider;
import com.qvqw.idp.auth.internal.TokenStore;
import com.qvqw.idp.auth.model.req.LoginReq;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void loginGetUserInfoLogout() throws Exception {
        // 因为 StringRedisTemplate 被 mock，TokenStore 用一个内存 map 替代
        Map<String, String> store = new HashMap<>();
        org.mockito.Mockito.when(stringRedisTemplate.opsForValue()).thenAnswer(inv -> {
            org.springframework.data.redis.core.ValueOperations<String, String> ops =
                    org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
            org.mockito.Mockito.doAnswer(call -> {
                store.put(call.getArgument(0), call.getArgument(1));
                return null;
            }).when(ops).set(org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.any(java.time.Duration.class));
            org.mockito.Mockito.when(ops.get(org.mockito.ArgumentMatchers.anyString()))
                    .thenAnswer(g -> store.get(g.getArgument(0)));
            return ops;
        });
        org.mockito.Mockito.when(stringRedisTemplate.hasKey(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> store.containsKey((String) call.getArgument(0)));
        org.mockito.Mockito.when(stringRedisTemplate.delete(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> {
                    String key = call.getArgument(0);
                    return store.remove(key) != null;
                });

        // 登录
        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        Map<?, ?> json = objectMapper.readValue(body, Map.class);
        Map<?, ?> data = (Map<?, ?>) json.get("data");
        String token = (String) data.get("token");
        assertThat(token).isNotBlank();

        // 携带 token 调用 user/info
        mockMvc.perform(get("/auth/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"));

        // 登出后 token 应失效
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/user/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithWrongPasswordReturns400() throws Exception {
        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("wrong");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("用户名或密码错误")));
    }
}
