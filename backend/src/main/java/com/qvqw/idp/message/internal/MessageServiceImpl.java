package com.qvqw.idp.message.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.message.Message;
import com.qvqw.idp.message.MessageLog;
import com.qvqw.idp.message.MessageService;
import com.qvqw.idp.message.model.query.MessageQuery;
import com.qvqw.idp.message.model.req.MessageCreateReq;
import com.qvqw.idp.message.model.resp.MessageResp;
import com.qvqw.idp.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息服务实现。
 *
 * <p>分页 + 已读状态需要按用户维度 join {@code idp_sys_message_log}，
 * 用 JPQL + 动态拼接（避免 null 参数下推）。</p>
 */
@Service
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageLogRepository messageLogRepository;
    private final UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    public MessageServiceImpl(MessageRepository messageRepository,
                              MessageLogRepository messageLogRepository,
                              UserService userService) {
        this.messageRepository = messageRepository;
        this.messageLogRepository = messageLogRepository;
        this.userService = userService;
    }

    /**
     * 发布消息：写主表 + 为每个收件人插入未读 {@code MessageLog}。
     *
     * <p>userIds 为空时，自动扩展为全体启用用户；最终列表为空则只保留消息主体、不产生 log。</p>
     *
     * @param req     消息正文
     * @param userIds 收件人 ID 列表，可为空
     * @return 新建消息 ID
     */
    @Override
    @Transactional
    public Long publish(MessageCreateReq req, List<Long> userIds) {
        Message message = new Message();
        message.setType(req.getType() == null ? 1 : req.getType());
        message.setTitle(req.getTitle());
        message.setContent(req.getContent());
        message.setPath(req.getPath());
        Long messageId = messageRepository.save(message).getId();

        List<Long> targets = (userIds == null || userIds.isEmpty())
                ? userService.listEnabledUserIds()
                : userIds.stream().distinct().toList();
        if (targets.isEmpty()) {
            return messageId;
        }
        List<MessageLog> logs = targets.stream()
                .map(uid -> new MessageLog(messageId, uid))
                .toList();
        messageLogRepository.saveAll(logs);
        return messageId;
    }

    @Override
    public PageResp<MessageResp> page(MessageQuery query, int page, int size) {
        Long userId = requireUserId();
        int pageIdx = Math.max(page - 1, 0);

        StringBuilder where = new StringBuilder(
                " from Message m, MessageLog l where l.messageId = m.id and l.userId = :uid");
        Map<String, Object> params = new HashMap<>();
        params.put("uid", userId);
        if (query != null && StringUtils.hasText(query.getTitle())) {
            where.append(" and lower(m.title) like :title");
            params.put("title", "%" + query.getTitle().trim().toLowerCase() + "%");
        }
        if (query != null && Boolean.TRUE.equals(query.getIsRead())) {
            where.append(" and l.readTime is not null");
        } else if (query != null && Boolean.FALSE.equals(query.getIsRead())) {
            where.append(" and l.readTime is null");
        }

        var dataQuery = entityManager.createQuery(
                "select m, l" + where + " order by m.id desc", Object[].class);
        var countQuery = entityManager.createQuery(
                "select count(m)" + where, Long.class);
        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });
        long total = countQuery.getSingleResult();
        dataQuery.setFirstResult(pageIdx * size);
        dataQuery.setMaxResults(size);

        List<MessageResp> list = new ArrayList<>();
        for (Object[] row : dataQuery.getResultList()) {
            Message m = (Message) row[0];
            MessageLog log = (MessageLog) row[1];
            MessageResp resp = toResp(m);
            resp.setIsRead(log.getReadTime() != null);
            resp.setReadTime(log.getReadTime());
            list.add(resp);
        }
        return new PageResp<>(list, total, page, size);
    }

    @Override
    public long unreadCount() {
        return messageLogRepository.countByUserIdAndReadTimeIsNull(requireUserId());
    }

    @Override
    @Transactional
    public void read(Long messageId) {
        Long userId = requireUserId();
        messageLogRepository.findByUserIdAndMessageId(userId, messageId).ifPresent(log -> {
            if (log.getReadTime() == null) {
                log.setReadTime(LocalDateTime.now());
                messageLogRepository.save(log);
            }
        });
    }

    @Override
    @Transactional
    public void readAll() {
        messageLogRepository.markAllReadByUserId(requireUserId(), LocalDateTime.now());
    }

    private static Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private static MessageResp toResp(Message m) {
        MessageResp resp = new MessageResp();
        resp.setId(m.getId());
        resp.setType(m.getType());
        resp.setTitle(m.getTitle());
        resp.setContent(m.getContent());
        resp.setPath(m.getPath());
        resp.setCreatedAt(m.getCreatedAt());
        return resp;
    }
}
