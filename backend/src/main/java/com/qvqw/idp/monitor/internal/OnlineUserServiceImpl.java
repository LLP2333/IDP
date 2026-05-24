package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.auth.AuthSessionService;
import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.monitor.OnlineSession;
import com.qvqw.idp.monitor.OnlineUserService;
import com.qvqw.idp.monitor.model.query.OnlineUserQuery;
import com.qvqw.idp.monitor.model.resp.OnlineUserResp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** 在线用户服务实现。 */
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private final OnlineSessionRepository repository;
    private final AuthSessionService authSessionService;

    public OnlineUserServiceImpl(OnlineSessionRepository repository,
                                 AuthSessionService authSessionService) {
        this.repository = repository;
        this.authSessionService = authSessionService;
    }

    /**
     * 记录登录成功后的在线会话。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void recordLogin(String token, String jti, Long userId, String username, String nickname,
                            HttpServletRequest request) {
        OnlineSession session = new OnlineSession();
        session.setToken(token);
        session.setJti(jti);
        session.setUserId(userId);
        session.setUsername(username);
        session.setNickname(nickname);
        String ua = request == null ? null : request.getHeader("User-Agent");
        String ip = request == null ? null : MonitorHttpUtils.clientIp(request);
        session.setIp(ip);
        session.setAddress(ip);
        session.setBrowser(MonitorHttpUtils.browser(ua));
        session.setOs(MonitorHttpUtils.os(ua));
        LocalDateTime now = LocalDateTime.now();
        session.setLoginTime(now);
        session.setLastActiveTime(now);
        repository.save(session);
    }

    /**
     * 刷新在线会话的最后活跃时间。
     *
     * <p>会话不存在时静默忽略，不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void touch(String token) {
        repository.findById(token).ifPresent(session -> {
            session.setLastActiveTime(LocalDateTime.now());
            repository.save(session);
        });
    }

    /**
     * 移除在线会话。
     *
     * <p>会话不存在时静默忽略，不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void remove(String token) {
        repository.deleteById(token);
    }

    /**
     * 分页查询在线用户，并清理 TokenStore 中已失效的会话。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public PageResp<OnlineUserResp> page(OnlineUserQuery query, int page, int size) {
        cleanupInvalidSessions();
        List<OnlineUserResp> all = repository.findAll(Sort.by(Sort.Direction.DESC, "loginTime"))
                .stream()
                .filter(s -> matches(s, query))
                .map(this::toResp)
                .toList();
        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, size);
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        return new PageResp<>(all.subList(from, to), all.size(), pageNo, pageSize);
    }

    /**
     * 强退在线用户，同时移除认证 TokenStore 与在线会话记录。
     *
     * <p>TokenStore 清理失败时仍会删除在线会话，不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void kickout(String token) {
        try {
            authSessionService.kickoutToken(token);
        } catch (Exception ignored) {
        }
        repository.deleteById(token);
    }

    private void cleanupInvalidSessions() {
        for (OnlineSession session : repository.findAll(PageRequest.of(0, 500)).getContent()) {
            if (!authSessionService.existsJti(session.getJti())) {
                repository.delete(session);
            }
        }
    }

    private boolean matches(OnlineSession session, OnlineUserQuery query) {
        if (query == null) {
            return true;
        }
        if (StringUtils.hasText(query.getNickname())) {
            String kw = query.getNickname().trim().toLowerCase();
            String username = session.getUsername() == null ? "" : session.getUsername().toLowerCase();
            String nickname = session.getNickname() == null ? "" : session.getNickname().toLowerCase();
            if (!username.contains(kw) && !nickname.contains(kw)) {
                return false;
            }
        }
        if (query.getLoginTime() != null && query.getLoginTime().size() >= 2) {
            LocalDateTime start = query.getLoginTime().get(0);
            LocalDateTime end = query.getLoginTime().get(1);
            if (start != null && session.getLoginTime().isBefore(start)) {
                return false;
            }
            if (end != null && session.getLoginTime().isAfter(end)) {
                return false;
            }
        }
        return true;
    }

    private OnlineUserResp toResp(OnlineSession session) {
        OnlineUserResp resp = new OnlineUserResp();
        resp.setToken(session.getToken());
        resp.setUsername(session.getUsername());
        resp.setNickname(session.getNickname());
        resp.setIp(session.getIp());
        resp.setAddress(session.getAddress());
        resp.setBrowser(session.getBrowser());
        resp.setOs(session.getOs());
        resp.setLoginTime(session.getLoginTime());
        resp.setLastActiveTime(session.getLastActiveTime());
        return resp;
    }
}
