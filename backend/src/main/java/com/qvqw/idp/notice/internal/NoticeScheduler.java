package com.qvqw.idp.notice.internal;

import com.qvqw.idp.notice.NoticeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时发布调度器。
 *
 * <p>每 60s 扫描一次 {@code idp_sys_notice}：把状态为 {@code PENDING} 且
 * {@code publish_time <= now} 的公告推进到 {@code PUBLISHED} 并触发消息分发。</p>
 *
 * <p>当前为单进程内调度；多实例部署后建议引入 Redis 分布式锁防止重复发布。</p>
 */
@Component
public class NoticeScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoticeScheduler.class);

    private final NoticeService noticeService;

    public NoticeScheduler(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 60s 一次的轮询任务。
     *
     * <p>逐条调用 {@link NoticeService#publishNow(Long)}；某条失败不影响其它公告。</p>
     */
    @Scheduled(fixedDelayString = "${idp.notice.scheduler.delay-ms:60000}",
            initialDelayString = "${idp.notice.scheduler.initial-delay-ms:30000}")
    public void publishDuePendingNotices() {
        List<Long> ids = noticeService.listPendingDueIds();
        if (ids.isEmpty()) {
            return;
        }
        log.info("[NoticeScheduler] 发现 {} 条待发布公告: {}", ids.size(), ids);
        for (Long id : ids) {
            try {
                noticeService.publishNow(id);
            } catch (Exception e) {
                log.warn("[NoticeScheduler] 公告 {} 发布失败: {}", id, e.getMessage(), e);
            }
        }
    }
}
