package com.qvqw.idp.monitor.internal;

import com.qvqw.idp.monitor.OnlineSession;
import org.springframework.data.jpa.repository.JpaRepository;

/** 在线会话 Repository。 */
public interface OnlineSessionRepository extends JpaRepository<OnlineSession, String> {
}
