package com.qvqw.idp.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qvqw.idp.auth.model.req.LoginReq;
import com.qvqw.idp.notice.model.req.NoticeReq;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 通知公告 Controller 集成测试。
 *
 * <p>覆盖：登录拿 token → 新增立即发布公告 → 列表 / 详情 / Dashboard / Popup / 删除。</p>
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class NoticeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;

    /**
     * 登录 admin 拿 token，并用一个内存 Map mock 掉 Redis 上的 TokenStore。
     */
    @BeforeEach
    void login() throws Exception {
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
                .thenAnswer(call -> store.remove((String) call.getArgument(0)) != null);

        LoginReq req = new LoginReq();
        req.setUsername("admin");
        req.setPassword("123456");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> json = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Map<?, ?> data = (Map<?, ?>) json.get("data");
        token = (String) data.get("token");
        assertThat(token).isNotBlank();
    }

    @Test
    void crudAndDashboardFlow() throws Exception {
        NoticeReq create = new NoticeReq();
        create.setTitle("集成测试公告");
        create.setContent("公告正文");
        create.setType("1");
        create.setNoticeScope(NoticeScope.ALL.getValue());
        create.setNoticeMethods(List.of(NoticeMethod.POPUP.getValue()));
        create.setIsTiming(false);
        create.setIsTop(true);
        create.setStatus(NoticeStatus.PUBLISHED.getValue());

        MvcResult addResult = mockMvc.perform(post("/system/notice")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();
        Map<?, ?> addJson = objectMapper.readValue(addResult.getResponse().getContentAsString(), Map.class);
        Number id = (Number) addJson.get("data");

        mockMvc.perform(get("/system/notice")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[?(@.title=='集成测试公告')]").exists());

        mockMvc.perform(get("/system/notice/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("集成测试公告"))
                .andExpect(jsonPath("$.data.content").value("公告正文"));

        mockMvc.perform(get("/system/notice/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='集成测试公告')]").exists());

        // 弹窗接口：刚发布的还未读 + method 含 POPUP，应能出现
        mockMvc.perform(get("/system/notice/popup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='集成测试公告')]").exists());

        // 标记已读
        mockMvc.perform(post("/system/notice/" + id + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 删除
        mockMvc.perform(delete("/system/notice")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/system/notice/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500));
    }
}
