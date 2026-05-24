package com.qvqw.idp.message.internal;

import com.qvqw.idp.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 消息主表 JPA Repository。
 */
public interface MessageRepository extends JpaRepository<Message, Long> {
}
