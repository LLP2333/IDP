package com.qvqw.idp.notice.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.message.MessageService;
import com.qvqw.idp.notice.Notice;
import com.qvqw.idp.notice.NoticeMethod;
import com.qvqw.idp.notice.NoticeScope;
import com.qvqw.idp.notice.NoticeStatus;
import com.qvqw.idp.notice.model.req.NoticeReq;
import com.qvqw.idp.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoticeServiceImplTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeLogRepository noticeLogRepository;

    @Mock
    private UserService userService;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NoticeReq baseReq() {
        NoticeReq req = new NoticeReq();
        req.setTitle("测试公告");
        req.setContent("hello");
        req.setType("1");
        req.setNoticeScope(NoticeScope.ALL.getValue());
        req.setNoticeMethods(List.of(NoticeMethod.SYSTEM_MESSAGE.getValue()));
        req.setIsTiming(false);
        req.setIsTop(false);
        req.setStatus(NoticeStatus.DRAFT.getValue());
        return req;
    }

    @Test
    void createDraftDoesNotPublishMessage() {
        Notice saved = new Notice();
        saved.setId(1L);
        when(noticeRepository.save(any(Notice.class))).thenReturn(saved);

        Long id = noticeService.create(baseReq());

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NoticeStatus.DRAFT.getValue());
        verify(messageService, never()).publish(any(), any());
    }

    @Test
    void createPublishedImmediatelyDispatchesSystemMessage() {
        NoticeReq req = baseReq();
        req.setStatus(NoticeStatus.PUBLISHED.getValue());
        Notice saved = new Notice();
        saved.setId(2L);
        saved.setStatus(NoticeStatus.PUBLISHED.getValue());
        saved.setNoticeMethods(List.of(NoticeMethod.SYSTEM_MESSAGE.getValue()));
        saved.setNoticeScope(NoticeScope.ALL.getValue());
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> {
            Notice n = inv.getArgument(0);
            n.setId(2L);
            return n;
        });

        noticeService.create(req);

        verify(messageService, times(1)).publish(any(), any());
    }

    @Test
    void createTimingShouldMapToPending() {
        NoticeReq req = baseReq();
        req.setStatus(NoticeStatus.PUBLISHED.getValue());
        req.setIsTiming(true);
        req.setPublishTime(LocalDateTime.now().plusHours(1));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> {
            Notice n = inv.getArgument(0);
            n.setId(3L);
            return n;
        });

        noticeService.create(req);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NoticeStatus.PENDING.getValue());
        verify(messageService, never()).publish(any(), any());
    }

    @Test
    void createTimingWithoutPublishTimeShouldThrow() {
        NoticeReq req = baseReq();
        req.setStatus(NoticeStatus.PUBLISHED.getValue());
        req.setIsTiming(true);

        assertThatThrownBy(() -> noticeService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("定时发布时间不能为空");
    }

    @Test
    void createTimingPastShouldThrow() {
        NoticeReq req = baseReq();
        req.setStatus(NoticeStatus.PUBLISHED.getValue());
        req.setIsTiming(true);
        req.setPublishTime(LocalDateTime.now().minusHours(1));

        assertThatThrownBy(() -> noticeService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能早于");
    }

    @Test
    void scopeUserWithoutUsersShouldThrow() {
        NoticeReq req = baseReq();
        req.setNoticeScope(NoticeScope.USER.getValue());
        req.setNoticeUsers(List.of());
        req.setStatus(NoticeStatus.DRAFT.getValue());

        assertThatThrownBy(() -> noticeService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("指定用户不能为空");
    }

    @Test
    void invalidNoticeMethodShouldThrow() {
        NoticeReq req = baseReq();
        req.setNoticeMethods(List.of(99));
        assertThatThrownBy(() -> noticeService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("通知方式");
    }

    @Test
    void updatePublishedCannotChangeScope() {
        Notice published = new Notice();
        published.setId(1L);
        published.setStatus(NoticeStatus.PUBLISHED.getValue());
        published.setNoticeScope(NoticeScope.ALL.getValue());
        published.setNoticeMethods(List.of(NoticeMethod.SYSTEM_MESSAGE.getValue()));
        published.setIsTiming(false);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(published));

        NoticeReq req = baseReq();
        req.setStatus(NoticeStatus.PUBLISHED.getValue());
        req.setNoticeScope(NoticeScope.USER.getValue());
        req.setNoticeUsers(List.of(100L));

        assertThatThrownBy(() -> noticeService.update(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许修改通知范围");
    }

    @Test
    void deleteShouldCascadeLog() {
        noticeService.delete(List.of(1L, 2L));
        verify(noticeLogRepository).deleteByNoticeIds(List.of(1L, 2L));
        verify(noticeRepository).deleteAllById(List.of(1L, 2L));
    }

    @Test
    void publishNowAdvancesPendingAndDispatches() {
        Notice pending = new Notice();
        pending.setId(5L);
        pending.setStatus(NoticeStatus.PENDING.getValue());
        pending.setNoticeMethods(List.of(NoticeMethod.SYSTEM_MESSAGE.getValue()));
        pending.setNoticeScope(NoticeScope.ALL.getValue());
        when(noticeRepository.findById(5L)).thenReturn(Optional.of(pending));

        noticeService.publishNow(5L);

        assertThat(pending.getStatus()).isEqualTo(NoticeStatus.PUBLISHED.getValue());
        verify(messageService).publish(any(), any());
    }

    @Test
    void listPendingDueIdsDelegates() {
        when(noticeRepository.findPendingDueIds(
                org.mockito.ArgumentMatchers.eq(NoticeStatus.PENDING.getValue()), any()))
                .thenReturn(List.of(1L, 2L));
        assertThat(noticeService.listPendingDueIds()).containsExactly(1L, 2L);
    }
}
