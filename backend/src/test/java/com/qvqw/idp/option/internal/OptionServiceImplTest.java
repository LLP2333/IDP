package com.qvqw.idp.option.internal;

import com.qvqw.idp.common.exception.BusinessException;
import com.qvqw.idp.option.OptionCategory;
import com.qvqw.idp.option.SystemOption;
import com.qvqw.idp.option.model.req.OptionReq;
import com.qvqw.idp.option.model.req.OptionValueResetReq;
import com.qvqw.idp.option.model.resp.OptionResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OptionServiceImplTest {

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private OptionServiceImpl optionService;

    private SystemOption titleOption;

    @BeforeEach
    void setUp() {
        titleOption = new SystemOption();
        titleOption.setId(1L);
        titleOption.setCategory(OptionCategory.SITE);
        titleOption.setCode("SITE_TITLE");
        titleOption.setDefaultValue("Default Title");
        titleOption.setName("系统名称");
    }

    @Test
    void listByCategoryReturnsMappedResp() {
        when(optionRepository.findByCategoryOrderByIdAsc(OptionCategory.SITE))
                .thenReturn(List.of(titleOption));
        List<OptionResp> resp = optionService.list(buildQuery(OptionCategory.SITE, null));
        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getValue()).isEqualTo("Default Title");
    }

    @Test
    void updateWithMismatchedIdCodeShouldFail() {
        when(optionRepository.findAllById(List.of(1L))).thenReturn(List.of(titleOption));
        OptionReq req = new OptionReq();
        req.setId(1L);
        req.setCode("SITE_OTHER");
        req.setValue("v");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不匹配");
    }

    @Test
    void updateMissingOptionShouldFail() {
        when(optionRepository.findAllById(List.of(9L))).thenReturn(List.of());
        OptionReq req = new OptionReq();
        req.setId(9L);
        req.setCode("SITE_TITLE");
        req.setValue("v");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("参数不存在");
    }

    @Test
    void updatePasswordPolicyOutOfRangeShouldFail() {
        SystemOption opt = new SystemOption();
        opt.setId(2L);
        opt.setCategory(OptionCategory.PASSWORD);
        opt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findAllById(List.of(2L))).thenReturn(List.of(opt));
        OptionReq req = new OptionReq();
        req.setId(2L);
        req.setCode("PASSWORD_MIN_LENGTH");
        req.setValue("5");
        assertThatThrownBy(() -> optionService.update(List.of(req)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("取值范围");
    }

    @Test
    void updatePasswordPolicyValid() {
        SystemOption opt = new SystemOption();
        opt.setId(2L);
        opt.setCategory(OptionCategory.PASSWORD);
        opt.setCode("PASSWORD_MIN_LENGTH");
        when(optionRepository.findAllById(List.of(2L))).thenReturn(List.of(opt));
        OptionReq req = new OptionReq();
        req.setId(2L);
        req.setCode("PASSWORD_MIN_LENGTH");
        req.setValue("12");
        optionService.update(List.of(req));
        verify(optionRepository).saveAll(any(Iterable.class));
        assertThat(opt.getValue()).isEqualTo("12");
    }

    @Test
    void resetWithoutAnyCriteriaShouldFail() {
        OptionValueResetReq req = new OptionValueResetReq();
        assertThatThrownBy(() -> optionService.resetValue(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请指定");
    }

    @Test
    void resetByCategory() {
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCategory(OptionCategory.SITE);
        optionService.resetValue(req);
        verify(optionRepository).resetByCategory(OptionCategory.SITE);
        verify(optionRepository, never()).resetByCodes(anyList());
    }

    @Test
    void resetByCodes() {
        OptionValueResetReq req = new OptionValueResetReq();
        req.setCodes(List.of("SITE_TITLE"));
        optionService.resetValue(req);
        verify(optionRepository).resetByCodes(List.of("SITE_TITLE"));
    }

    @Test
    void getValueShouldReturnEffectiveValue() {
        titleOption.setValue("Real Title");
        when(optionRepository.findByCode("SITE_TITLE")).thenReturn(Optional.of(titleOption));
        String v = optionService.getValue("SITE_TITLE", s -> s);
        assertThat(v).isEqualTo("Real Title");
    }

    @Test
    void getIntOrDefaultFallback() {
        when(optionRepository.findByCode("X")).thenReturn(Optional.empty());
        assertThat(optionService.getIntOrDefault("X", 7)).isEqualTo(7);
    }

    @Test
    void validateImageShouldRejectPlainText() {
        assertThatThrownBy(() -> optionService.validateImage("not-image"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validateImageShouldRejectTooLarge() {
        // ~2MB
        String big = "A".repeat(2 * 1024 * 1024);
        assertThatThrownBy(() -> optionService.validateImage("data:image/png;base64," + big))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1MB");
    }

    @Test
    void validateImageOk() {
        assertThat(optionService.validateImage("data:image/png;base64,abcd")).startsWith("data:image/png");
    }

    private static com.qvqw.idp.option.model.query.OptionQuery buildQuery(OptionCategory category,
                                                                         List<String> codes) {
        com.qvqw.idp.option.model.query.OptionQuery q = new com.qvqw.idp.option.model.query.OptionQuery();
        q.setCategory(category);
        q.setCodes(codes);
        return q;
    }
}
