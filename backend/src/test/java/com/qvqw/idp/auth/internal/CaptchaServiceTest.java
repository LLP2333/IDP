package com.qvqw.idp.auth.internal;

import com.qvqw.idp.auth.model.resp.CaptchaResp;
import com.qvqw.idp.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptchaServiceTest {

    private CaptchaService service;

    @BeforeEach
    void setUp() {
        // Redis 为 null，触发 fallback 内存存储
        service = new CaptchaService(null, true);
    }

    @Test
    void generateShouldReturnValidSvgDataUrl() {
        CaptchaResp resp = service.generate();
        assertThat(resp.getCaptchaId()).isNotBlank();
        assertThat(resp.getImage()).startsWith("data:image/svg+xml;base64,");
        // 解码出来必须是 SVG
        String b64 = resp.getImage().substring("data:image/svg+xml;base64,".length());
        String svg = new String(Base64.getDecoder().decode(b64));
        assertThat(svg).contains("<svg ").contains("</svg>");
    }

    @Test
    void consumeBlankShouldFail() {
        assertThatThrownBy(() -> service.consume("", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请输入");
    }

    @Test
    void consumeUnknownShouldFail() {
        assertThatThrownBy(() -> service.consume("not-exist", "ABCD"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void consumeOnceShouldInvalidate() {
        CaptchaResp resp = service.generate();
        // 第二次使用必然失败（不论输入是什么）
        // 第一次先用错的 → 也被销毁
        String anyCode = "WRONG-INPUT";
        assertThatThrownBy(() -> service.consume(resp.getCaptchaId(), anyCode))
                .isInstanceOf(BusinessException.class);
        // 再消费同一个 ID
        assertThatThrownBy(() -> service.consume(resp.getCaptchaId(), anyCode))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已过期");
    }

    @Test
    void consumeCorrectShouldPass() {
        CaptchaResp resp = service.generate();
        // 从 fallback store 拿到正确值
        String key = "idp:auth:captcha:" + resp.getCaptchaId();
        String code = service.snapshotForTest().get(key);
        assertThat(code).isNotBlank();
        // 大小写不敏感
        service.consume(resp.getCaptchaId(), code.toLowerCase());
    }
}
