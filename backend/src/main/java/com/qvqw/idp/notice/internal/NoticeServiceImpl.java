package com.qvqw.idp.notice.internal;

import com.qvqw.idp.common.api.PageResp;
import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.common.security.UserContextHolder;
import com.qvqw.idp.message.MessageService;
import com.qvqw.idp.message.model.req.MessageCreateReq;
import com.qvqw.idp.notice.Notice;
import com.qvqw.idp.notice.NoticeLog;
import com.qvqw.idp.notice.NoticeMethod;
import com.qvqw.idp.notice.NoticeScope;
import com.qvqw.idp.notice.NoticeService;
import com.qvqw.idp.notice.NoticeStatus;
import com.qvqw.idp.notice.model.query.NoticeQuery;
import com.qvqw.idp.notice.model.req.NoticeReq;
import com.qvqw.idp.notice.model.resp.DashboardNoticeResp;
import com.qvqw.idp.notice.model.resp.NoticeDetailResp;
import com.qvqw.idp.notice.model.resp.NoticeResp;
import com.qvqw.idp.user.UserService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 公告业务实现。
 *
 * <p>状态机：</p>
 * <pre>
 *   DRAFT --(立即发布)--> PUBLISHED  ── fanout SYSTEM_MESSAGE
 *   DRAFT --(定时)----> PENDING ──(到点)─> PUBLISHED
 * </pre>
 *
 * <p>已发布的公告锁定通知范围 / 方式 / 定时设置，避免重新触发分发。</p>
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeLogRepository noticeLogRepository;
    private final UserService userService;
    private final MessageService messageService;

    public NoticeServiceImpl(NoticeRepository noticeRepository,
                             NoticeLogRepository noticeLogRepository,
                             UserService userService,
                             MessageService messageService) {
        this.noticeRepository = noticeRepository;
        this.noticeLogRepository = noticeLogRepository;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public PageResp<NoticeResp> page(NoticeQuery query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "isTop").and(Sort.by(Sort.Direction.DESC, "id")));
        Specification<Notice> spec = buildSpec(query);
        Page<Notice> result = noticeRepository.findAll(spec, pageable);

        List<Long> creatorIds = result.getContent().stream()
                .map(Notice::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> creatorMap = creatorIds.isEmpty() ? Map.of() : userService.mapDisplayNames(creatorIds);

        return PageResp.from(result, notice -> toResp(notice, creatorMap));
    }

    private static Specification<Notice> buildSpec(NoticeQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null) {
                if (StringUtils.hasText(query.getTitle())) {
                    predicates.add(cb.like(cb.lower(root.get("title")),
                            "%" + query.getTitle().trim().toLowerCase() + "%"));
                }
                if (StringUtils.hasText(query.getType())) {
                    predicates.add(cb.equal(root.get("type"), query.getType()));
                }
                if (query.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), query.getStatus()));
                }
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public NoticeDetailResp get(Long id) {
        Notice notice = loadNotice(id);
        Map<Long, String> creatorMap = notice.getCreatedBy() == null
                ? Map.of()
                : userService.mapDisplayNames(List.of(notice.getCreatedBy()));
        return toDetail(notice, creatorMap);
    }

    @Override
    @Transactional
    public Long create(NoticeReq req) {
        validateMethods(req.getNoticeMethods());
        Notice notice = new Notice();
        applyReq(notice, req);

        Integer reqStatus = req.getStatus() == null ? NoticeStatus.DRAFT.getValue() : req.getStatus();
        Boolean timing = Boolean.TRUE.equals(req.getIsTiming());
        if (NoticeStatus.DRAFT.getValue().equals(reqStatus)) {
            notice.setStatus(NoticeStatus.DRAFT.getValue());
        } else if (NoticeStatus.PUBLISHED.getValue().equals(reqStatus)) {
            if (timing) {
                if (req.getPublishTime() == null) {
                    throw new BusinessException("定时发布时间不能为空");
                }
                if (req.getPublishTime().isBefore(LocalDateTime.now())) {
                    throw new BusinessException("定时发布时间不能早于当前时间");
                }
                notice.setStatus(NoticeStatus.PENDING.getValue());
                notice.setPublishTime(req.getPublishTime());
            } else {
                notice.setStatus(NoticeStatus.PUBLISHED.getValue());
                notice.setPublishTime(LocalDateTime.now());
            }
        } else {
            throw new BusinessException("公告状态无效");
        }

        Long id = noticeRepository.save(notice).getId();
        if (NoticeStatus.PUBLISHED.getValue().equals(notice.getStatus())) {
            doPublishMessage(notice);
        }
        return id;
    }

    @Override
    @Transactional
    public void update(Long id, NoticeReq req) {
        Notice notice = loadNotice(id);
        validateMethods(req.getNoticeMethods());
        NoticeStatus oldStatus = NoticeStatus.of(notice.getStatus());
        if (oldStatus == NoticeStatus.PUBLISHED) {
            // 已发布：锁定通知范围 / 方式 / 定时设置
            if (!Objects.equals(req.getNoticeScope(), notice.getNoticeScope())) {
                throw new BusinessException("公告已发布，不允许修改通知范围");
            }
            if (!Objects.equals(Boolean.TRUE.equals(req.getIsTiming()), Boolean.TRUE.equals(notice.getIsTiming()))) {
                throw new BusinessException("公告已发布，不允许修改定时设置");
            }
            if (!sameMethods(req.getNoticeMethods(), notice.getNoticeMethods())) {
                throw new BusinessException("公告已发布，不允许修改通知方式");
            }
            if (NoticeScope.USER.getValue().equals(notice.getNoticeScope())
                    && !sameUsers(req.getNoticeUsers(), notice.getNoticeUsers())) {
                throw new BusinessException("公告已发布，不允许修改通知用户");
            }
            // 已发布只允许改 标题 / 正文 / 分类 / 置顶
            notice.setTitle(req.getTitle());
            notice.setContent(req.getContent());
            notice.setType(req.getType());
            notice.setIsTop(Boolean.TRUE.equals(req.getIsTop()));
            noticeRepository.save(notice);
            return;
        }

        // DRAFT / PENDING
        applyReq(notice, req);

        Integer reqStatus = req.getStatus() == null ? notice.getStatus() : req.getStatus();
        Boolean timing = Boolean.TRUE.equals(req.getIsTiming());
        boolean willPublishNow = false;
        if (NoticeStatus.DRAFT.getValue().equals(reqStatus)) {
            notice.setStatus(NoticeStatus.DRAFT.getValue());
            notice.setPublishTime(null);
        } else if (NoticeStatus.PUBLISHED.getValue().equals(reqStatus)) {
            if (timing) {
                if (req.getPublishTime() == null) {
                    throw new BusinessException("定时发布时间不能为空");
                }
                if (req.getPublishTime().isBefore(LocalDateTime.now())) {
                    throw new BusinessException("定时发布时间不能早于当前时间");
                }
                notice.setStatus(NoticeStatus.PENDING.getValue());
                notice.setPublishTime(req.getPublishTime());
            } else {
                notice.setStatus(NoticeStatus.PUBLISHED.getValue());
                notice.setPublishTime(LocalDateTime.now());
                willPublishNow = true;
            }
        } else {
            throw new BusinessException("公告状态无效");
        }
        noticeRepository.save(notice);
        if (willPublishNow) {
            doPublishMessage(notice);
        }
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        noticeLogRepository.deleteByNoticeIds(ids);
        noticeRepository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public void publishNow(Long id) {
        Notice notice = loadNotice(id);
        if (!NoticeStatus.PENDING.getValue().equals(notice.getStatus())) {
            return;
        }
        notice.setStatus(NoticeStatus.PUBLISHED.getValue());
        if (notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }
        noticeRepository.save(notice);
        doPublishMessage(notice);
    }

    @Override
    public List<NoticeDetailResp> listPopupForCurrentUser() {
        Long userId = requireUserId();
        // 找出所有已发布、method 含 POPUP、且对当前用户可见的公告
        List<Notice> candidates = noticeRepository.findAll().stream()
                .filter(n -> NoticeStatus.PUBLISHED.getValue().equals(n.getStatus()))
                .filter(n -> n.getNoticeMethods() != null
                        && n.getNoticeMethods().contains(NoticeMethod.POPUP.getValue()))
                .filter(n -> NoticeScope.ALL.getValue().equals(n.getNoticeScope())
                        || (n.getNoticeUsers() != null && n.getNoticeUsers().contains(userId)))
                .sorted(Comparator.comparing(Notice::getPublishTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Long> candidateIds = candidates.stream().map(Notice::getId).toList();
        Set<Long> readIds = new HashSet<>(noticeLogRepository.findReadNoticeIdsByUser(userId, candidateIds));
        List<Notice> unread = candidates.stream()
                .filter(n -> !readIds.contains(n.getId()))
                .toList();
        if (unread.isEmpty()) {
            return List.of();
        }
        List<Long> creatorIds = unread.stream()
                .map(Notice::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> creatorMap = creatorIds.isEmpty() ? Map.of() : userService.mapDisplayNames(creatorIds);
        return unread.stream().map(n -> toDetail(n, creatorMap)).toList();
    }

    @Override
    @Transactional
    public void read(Long id) {
        Long userId = requireUserId();
        if (!noticeRepository.existsById(id)) {
            return;
        }
        noticeLogRepository.findByUserIdAndNoticeId(userId, id).ifPresentOrElse(log -> {
            if (log.getReadTime() == null) {
                log.setReadTime(LocalDateTime.now());
                noticeLogRepository.save(log);
            }
        }, () -> noticeLogRepository.save(new NoticeLog(id, userId, LocalDateTime.now())));
    }

    @Override
    public List<DashboardNoticeResp> listDashboard(int limit) {
        Long userId = UserContextHolder.getUserId();
        int max = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, max,
                Sort.by(Sort.Direction.DESC, "isTop").and(Sort.by(Sort.Direction.DESC, "publishTime")));
        Specification<Notice> spec = (root, cq, cb) -> cb.and(
                cb.equal(root.get("status"), NoticeStatus.PUBLISHED.getValue()),
                cb.or(cb.equal(root.get("noticeScope"), NoticeScope.ALL.getValue()),
                        cb.equal(root.get("noticeScope"), NoticeScope.USER.getValue()))
        );
        List<Notice> notices = noticeRepository.findAll(spec, pageable).getContent().stream()
                .filter(n -> userId == null
                        || NoticeScope.ALL.getValue().equals(n.getNoticeScope())
                        || (n.getNoticeUsers() != null && n.getNoticeUsers().contains(userId)))
                .toList();
        List<Long> ids = notices.stream().map(Notice::getId).toList();
        Set<Long> readIds = (userId == null || ids.isEmpty())
                ? Set.of()
                : new HashSet<>(noticeLogRepository.findReadNoticeIdsByUser(userId, ids));
        return notices.stream().map(n -> {
            DashboardNoticeResp r = new DashboardNoticeResp();
            r.setId(n.getId());
            r.setTitle(n.getTitle());
            r.setType(n.getType());
            r.setIsTop(n.getIsTop());
            r.setPublishTime(n.getPublishTime());
            r.setIsRead(readIds.contains(n.getId()));
            return r;
        }).toList();
    }

    @Override
    public List<Long> listPendingDueIds() {
        return noticeRepository.findPendingDueIds(NoticeStatus.PENDING.getValue(), LocalDateTime.now());
    }

    /** 校验通知方式整数值合法（仅允许 1/2）。 */
    private static void validateMethods(List<Integer> methods) {
        if (methods == null || methods.isEmpty()) {
            return;
        }
        List<Integer> valid = List.of(NoticeMethod.SYSTEM_MESSAGE.getValue(), NoticeMethod.POPUP.getValue());
        for (Integer m : methods) {
            if (!valid.contains(m)) {
                throw new BusinessException("通知方式 [" + m + "] 不正确");
            }
        }
    }

    /** 把 req 中的可变字段（不含 status/publishTime 推导）拷贝到 entity。 */
    private static void applyReq(Notice notice, NoticeReq req) {
        notice.setTitle(req.getTitle());
        notice.setContent(req.getContent());
        notice.setType(req.getType());
        notice.setNoticeScope(req.getNoticeScope());
        if (NoticeScope.USER.getValue().equals(req.getNoticeScope())) {
            if (req.getNoticeUsers() == null || req.getNoticeUsers().isEmpty()) {
                throw new BusinessException("指定用户不能为空");
            }
            notice.setNoticeUsers(req.getNoticeUsers().stream().distinct().toList());
        } else {
            notice.setNoticeUsers(null);
        }
        notice.setNoticeMethods(req.getNoticeMethods() == null
                ? null
                : req.getNoticeMethods().stream().distinct().toList());
        notice.setIsTiming(Boolean.TRUE.equals(req.getIsTiming()));
        notice.setIsTop(Boolean.TRUE.equals(req.getIsTop()));
    }

    /**
     * 把已发布的公告 fanout 到收件箱：仅当 noticeMethods 含 SYSTEM_MESSAGE 时生效。
     */
    private void doPublishMessage(Notice notice) {
        List<Integer> methods = notice.getNoticeMethods();
        if (methods == null || !methods.contains(NoticeMethod.SYSTEM_MESSAGE.getValue())) {
            return;
        }
        MessageCreateReq req = new MessageCreateReq(
                "公告通知",
                "您收到一条公告通知：" + notice.getTitle(),
                "/admin/system/notice/view?id=" + notice.getId());
        messageService.publish(req, NoticeScope.USER.getValue().equals(notice.getNoticeScope())
                ? notice.getNoticeUsers()
                : null);
    }

    private Notice loadNotice(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在"));
    }

    private static boolean sameMethods(List<Integer> a, List<Integer> b) {
        Set<Integer> aSet = a == null ? Set.of() : new HashSet<>(a);
        Set<Integer> bSet = b == null ? Set.of() : new HashSet<>(b);
        return aSet.equals(bSet);
    }

    private static boolean sameUsers(List<Long> a, List<Long> b) {
        Set<Long> aSet = a == null ? Set.of() : new HashSet<>(a);
        Set<Long> bSet = b == null ? Set.of() : new HashSet<>(b);
        return aSet.equals(bSet);
    }

    private NoticeResp toResp(Notice n, Map<Long, String> creatorMap) {
        NoticeResp r = new NoticeResp();
        fillBase(r, n, creatorMap);
        return r;
    }

    private NoticeDetailResp toDetail(Notice n, Map<Long, String> creatorMap) {
        NoticeDetailResp r = new NoticeDetailResp();
        fillBase(r, n, creatorMap);
        r.setContent(n.getContent());
        r.setNoticeUsers(n.getNoticeUsers());
        return r;
    }

    private void fillBase(NoticeResp r, Notice n, Map<Long, String> creatorMap) {
        r.setId(n.getId());
        r.setTitle(n.getTitle());
        r.setType(n.getType());
        r.setNoticeScope(n.getNoticeScope());
        r.setNoticeMethods(n.getNoticeMethods());
        r.setIsTiming(n.getIsTiming());
        r.setPublishTime(n.getPublishTime());
        r.setIsTop(n.getIsTop());
        r.setStatus(n.getStatus());
        r.setCreatedBy(n.getCreatedBy());
        r.setCreatedAt(n.getCreatedAt());
        r.setUpdatedAt(n.getUpdatedAt());
        if (n.getCreatedBy() != null) {
            r.setCreateUserString(creatorMap.get(n.getCreatedBy()));
        }
    }

    private static Long requireUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
