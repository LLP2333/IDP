package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.auth.AuthSessionService;
import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.monitor.OnlineSession;
import com.qvqw.idp.monitor.model.query.OnlineUserQuery;
import com.qvqw.idp.monitor.model.resp.OnlineUserResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnlineUserServiceImplTest {

    @Mock
    private OnlineSessionRepository repository;

    @Mock
    private AuthSessionService authSessionService;

    @InjectMocks
    private OnlineUserServiceImpl service;

    @Test
    void recordLoginShouldPersistSessionWithRequestInfo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0 Mac OS X");

        service.recordLogin("token-1", "jti-1", 1L, "admin", "管理员", request);

        ArgumentCaptor<OnlineSession> captor = ArgumentCaptor.forClass(OnlineSession.class);
        verify(repository).save(captor.capture());
        OnlineSession saved = captor.getValue();
        assertThat(saved.getToken()).isEqualTo("token-1");
        assertThat(saved.getJti()).isEqualTo("jti-1");
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getNickname()).isEqualTo("管理员");
        assertThat(saved.getIp()).isEqualTo("10.0.0.1");
        assertThat(saved.getBrowser()).isEqualTo("Chrome");
        assertThat(saved.getOs()).isEqualTo("macOS");
        assertThat(saved.getLoginTime()).isNotNull();
        assertThat(saved.getLastActiveTime()).isNotNull();
    }

    @Test
    void pageShouldCleanupInvalidSessionsAndFilterByNickname() {
        OnlineSession invalid = session("token-old", "jti-old", "old", "旧用户",
                LocalDateTime.now().minusDays(2));
        OnlineSession matched = session("token-admin", "jti-admin", "admin", "管理员",
                LocalDateTime.now().minusHours(1));
        OnlineSession other = session("token-user", "jti-user", "user", "普通用户",
                LocalDateTime.now().minusMinutes(30));

        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(invalid)));
        when(authSessionService.existsJti("jti-old")).thenReturn(false);
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(matched, other));

        OnlineUserQuery query = new OnlineUserQuery();
        query.setNickname("admin");
        PageResp<OnlineUserResp> page = service.page(query, 1, 10);

        verify(repository).delete(invalid);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(OnlineUserResp::getUsername).containsExactly("admin");
    }

    @Test
    void kickoutShouldRemoveTokenEvenWhenAuthStoreFails() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("bad token"))
                .when(authSessionService).kickoutToken("broken-token");

        service.kickout("broken-token");

        verify(authSessionService).kickoutToken("broken-token");
        verify(repository).deleteById("broken-token");
    }

    @Test
    void touchShouldRefreshExistingSession() {
        OnlineSession session = session("token-1", "jti-1", "admin", "管理员", LocalDateTime.now());
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);
        session.setLastActiveTime(before);
        when(repository.findById("token-1")).thenReturn(Optional.of(session));

        service.touch("token-1");

        assertThat(session.getLastActiveTime()).isAfter(before);
        verify(repository).save(session);
    }

    private static OnlineSession session(String token, String jti, String username, String nickname,
                                         LocalDateTime loginTime) {
        OnlineSession session = new OnlineSession();
        session.setToken(token);
        session.setJti(jti);
        session.setUserId(1L);
        session.setUsername(username);
        session.setNickname(nickname);
        session.setIp("127.0.0.1");
        session.setAddress("127.0.0.1");
        session.setBrowser("Chrome");
        session.setOs("macOS");
        session.setLoginTime(loginTime);
        session.setLastActiveTime(loginTime);
        return session;
    }
}
