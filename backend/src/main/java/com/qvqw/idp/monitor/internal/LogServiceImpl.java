package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.monitor.LogService;
import com.qvqw.idp.monitor.OperationLog;
import com.qvqw.idp.monitor.model.query.LogQuery;
import com.qvqw.idp.monitor.model.resp.LogDetailResp;
import com.qvqw.idp.monitor.model.resp.LogResp;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 系统日志服务实现。 */
@Service
public class LogServiceImpl implements LogService {

    private final OperationLogRepository repository;

    public LogServiceImpl(OperationLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 记录登录成功或失败日志。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void recordLogin(String username, boolean success, String errorMsg, HttpServletRequest request) {
        OperationLog log = new OperationLog();
        log.setTraceId(UUID.randomUUID().toString());
        log.setModule("登录");
        log.setDescription(success ? "登录成功" : "登录失败");
        log.setStatus(success ? 1 : 2);
        log.setStatusCode(success ? 200 : 400);
        log.setErrorMsg(errorMsg);
        log.setCreateUserString(username);
        log.setCreateTime(LocalDateTime.now());
        log.setTimeTaken(0L);
        String ua = request == null ? null : request.getHeader("User-Agent");
        String ip = request == null ? null : MonitorHttpUtils.clientIp(request);
        log.setIp(ip);
        log.setAddress(ip);
        log.setBrowser(MonitorHttpUtils.browser(ua));
        log.setOs(MonitorHttpUtils.os(ua));
        log.setRequestMethod(request == null ? null : request.getMethod());
        log.setRequestUrl(request == null ? null : request.getRequestURI());
        repository.save(log);
    }

    /**
     * 保存系统操作日志。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional
    public void record(OperationLog log) {
        repository.save(log);
    }

    /**
     * 分页查询系统日志。
     *
     * <p>本方法不主动抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public PageResp<LogResp> page(LogQuery query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "createTime"));
        return PageResp.from(repository.findAll(spec(query), pageRequest), this::toResp);
    }

    /**
     * 查询系统日志详情。
     *
     * <p>日志不存在时抛出 {@code BusinessException}。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public LogDetailResp get(Long id) {
        OperationLog log = repository.findById(id)
                .orElseThrow(() -> new BusinessException("日志不存在"));
        LogDetailResp resp = new LogDetailResp();
        fillBase(resp, log);
        resp.setTraceId(log.getTraceId());
        resp.setRequestUrl(log.getRequestUrl());
        resp.setRequestMethod(log.getRequestMethod());
        resp.setRequestHeaders(log.getRequestHeaders());
        resp.setRequestBody(log.getRequestBody());
        resp.setStatusCode(log.getStatusCode());
        resp.setResponseHeaders(log.getResponseHeaders());
        resp.setResponseBody(log.getResponseBody());
        return resp;
    }

    private Specification<OperationLog> spec(LogQuery query) {
        return (root, cq, cb) -> {
            if (query == null) {
                return cb.conjunction();
            }
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(query.getDescription())) {
                ps.add(cb.like(root.get("description"), "%" + query.getDescription().trim() + "%"));
            }
            if (StringUtils.hasText(query.getModule())) {
                ps.add(cb.equal(root.get("module"), query.getModule().trim()));
            }
            if (StringUtils.hasText(query.getIp())) {
                String kw = "%" + query.getIp().trim() + "%";
                ps.add(cb.or(cb.like(root.get("ip"), kw), cb.like(root.get("address"), kw)));
            }
            if (StringUtils.hasText(query.getCreateUserString())) {
                ps.add(cb.like(root.get("createUserString"), "%" + query.getCreateUserString().trim() + "%"));
            }
            if (query.getStatus() != null) {
                ps.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getCreateTime() != null && query.getCreateTime().size() >= 2) {
                LocalDateTime start = query.getCreateTime().get(0);
                LocalDateTime end = query.getCreateTime().get(1);
                if (start != null) {
                    ps.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
                }
                if (end != null) {
                    ps.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
                }
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private LogResp toResp(OperationLog log) {
        LogResp resp = new LogResp();
        fillBase(resp, log);
        return resp;
    }

    private void fillBase(LogResp resp, OperationLog log) {
        resp.setId(String.valueOf(log.getId()));
        resp.setDescription(log.getDescription());
        resp.setModule(log.getModule());
        resp.setTimeTaken(log.getTimeTaken());
        resp.setIp(log.getIp());
        resp.setAddress(log.getAddress());
        resp.setBrowser(log.getBrowser());
        resp.setOs(log.getOs());
        resp.setStatus(log.getStatus());
        resp.setErrorMsg(log.getErrorMsg());
        resp.setCreateUserString(log.getCreateUserString());
        resp.setCreateTime(log.getCreateTime());
    }
}
