package com.qvqw.idp.message.internal;

import com.qvqw.idp.message.Message;
import com.qvqw.idp.message.MessageLog;
import com.qvqw.idp.message.model.req.MessageCreateReq;
import com.qvqw.idp.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageLogRepository messageLogRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void publishWithSpecificUsersInsertsLogPerUser() {
        Message saved = new Message();
        saved.setId(9L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        MessageCreateReq req = new MessageCreateReq("标题", "正文", "/path");
        Long id = messageService.publish(req, List.of(1L, 2L, 2L));

        assertThat(id).isEqualTo(9L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MessageLog>> captor =
                ArgumentCaptor.forClass((Class<List<MessageLog>>) (Class<?>) List.class);
        verify(messageLogRepository).saveAll(captor.capture());
        // 去重后只剩 2 条
        assertThat(captor.getValue()).extracting(MessageLog::getUserId)
                .containsExactly(1L, 2L);
    }

    @Test
    void publishWithEmptyUsersExpandsToAllEnabled() {
        Message saved = new Message();
        saved.setId(10L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(userService.listEnabledUserIds()).thenReturn(List.of(7L, 8L));

        messageService.publish(new MessageCreateReq("t", "c", null), null);

        verify(userService).listEnabledUserIds();
        verify(messageLogRepository).saveAll(any());
    }

    @Test
    void publishWhenNoTargetsKeepsOnlyMainRecord() {
        Message saved = new Message();
        saved.setId(11L);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(userService.listEnabledUserIds()).thenReturn(List.of());

        Long id = messageService.publish(new MessageCreateReq("t", "c", null), null);

        assertThat(id).isEqualTo(11L);
        verify(messageLogRepository, never()).saveAll(any());
    }
}
