package com.qvqw.idp.notice.internal;

import com.qvqw.idp.notice.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告主表 JPA Repository。
 */
public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    /**
     * 列出全部 status / publish_time <= now 的到期待发布公告。
     *
     * @param status      状态值（{@link com.qvqw.idp.notice.NoticeStatus#PENDING}）
     * @param now         当前时间
     * @return 到期公告 ID 列表
     */
    @org.springframework.data.jpa.repository.Query(
            "select n.id from Notice n where n.status = :status and n.publishTime <= :now")
    List<Long> findPendingDueIds(@org.springframework.data.repository.query.Param("status") Integer status,
                                 @org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
